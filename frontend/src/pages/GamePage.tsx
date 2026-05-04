import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { captureMatchCards, createMatch, dealMatchRound, exitMatch, getMatch, playMatchCard } from '../api/matchesApi';
import GameBoard from '../components/GameBoard';
import type { Player } from '../types/game';
import type { MatchState } from '../types/match';

export default function GamePage() {
  const { matchId } = useParams();
  const navigate = useNavigate();
  const [match, setMatch] = useState<MatchState | null>(null);
  const [selectedTableCards, setSelectedTableCards] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadMatch() {
      try {
        setError(null);
        const loadedMatch = matchId ? await getMatch(matchId) : await createMatch();
        if (cancelled) {
          return;
        }
        setMatch(loadedMatch);
        if (!matchId) {
          navigate(`/game/${loadedMatch.id}`, { replace: true });
        }
      } catch (caught) {
        if (!cancelled) {
          setError(caught instanceof Error ? caught.message : 'Match could not be loaded.');
        }
      }
    }

    loadMatch();

    return () => {
      cancelled = true;
    };
  }, [matchId, navigate]);

  async function run(action: () => Promise<MatchState>) {
    try {
      setError(null);
      const nextMatch = await action();
      setMatch(nextMatch);
      return nextMatch;
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Something went wrong.');
      return null;
    }
  }

  async function handleNewGame() {
    setSelectedTableCards([]);
    const nextMatch = await run(createMatch);
    if (nextMatch) {
      navigate(`/game/${nextMatch.id}`, { replace: true });
    }
  }

  async function handleDeal() {
    if (!match) {
      return;
    }
    setSelectedTableCards([]);
    await run(() => dealMatchRound(match.id, match.currentRound.id));
  }

  async function handlePlayCard(player: Player, cardId: string) {
    if (!match) {
      return;
    }
    const nextMatch = await run(() => playMatchCard(match.id, match.currentRound.id, player, cardId));
    if (nextMatch) {
      setSelectedTableCards([]);
    }
  }

  async function handleCapture() {
    const game = match?.currentRound.gameState;
    if (!game?.pendingCapturePlayer) {
      setError('Drop a card before capturing.');
      return;
    }
    if (selectedTableCards.length === 0) {
      setError('Select at least one table card to capture.');
      return;
    }

    const nextMatch = await run(() =>
      captureMatchCards(match!.id, match!.currentRound.id, game.pendingCapturePlayer!, selectedTableCards)
    );
    if (nextMatch) {
      setSelectedTableCards([]);
    }
  }

  async function handleExitMatch() {
    if (!match) {
      return;
    }
    await run(() => exitMatch(match.id));
  }

  function handleToggleTableCard(cardId: string) {
    setSelectedTableCards((current) =>
      current.includes(cardId) ? current.filter((id) => id !== cardId) : [...current, cardId]
    );
  }

  if (!match && error) {
    return (
      <main className="app-shell">
        <section className="game-load-error">
          <p className="eyebrow">Pasoor</p>
          <h1>Game could not be loaded</h1>
          <p className="error-message">{error}</p>
          <button type="button" onClick={handleNewGame}>
            Try again
          </button>
        </section>
      </main>
    );
  }

  if (!match) {
    return <main className="loading">Loading Pasoor...</main>;
  }

  return (
    <main className="app-shell">
      <GameBoard
        game={match.currentRound.gameState}
        match={match}
        selectedTableCards={selectedTableCards}
        error={error}
        onDeal={handleDeal}
        onPlayCard={handlePlayCard}
        onToggleTableCard={handleToggleTableCard}
        onCapture={handleCapture}
        onNewGame={handleNewGame}
        onExitMatch={handleExitMatch}
      />
    </main>
  );
}
