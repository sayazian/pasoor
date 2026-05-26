import { useEffect, useRef, useState } from 'react';
import type { CSSProperties, ReactNode } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import {
  acknowledgeRound,
  captureMatchCards,
  chooseMatchEnd,
  createMatch,
  dealMatchRound,
  exitMatch,
  getMatch,
  playMatchCard
} from '../api/matchesApi';
import GameBoard from '../components/GameBoard';
import ScoreBoard from '../components/ScoreBoard';
import type { Card, GameState, Player } from '../types/game';
import type { CaptureAnimation } from '../components/GameBoard';
import type { MatchEndChoice, MatchPlayer, MatchState, RoundState } from '../types/match';

export default function GamePage({ captureAnimationEnabled = true }: { captureAnimationEnabled?: boolean }) {
  const { matchId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const inviteToken = typeof location.state === 'object' && location.state !== null && 'inviteToken' in location.state
    ? String(location.state.inviteToken)
    : null;
  const [match, setMatch] = useState<MatchState | null>(null);
  const [selectedTableCards, setSelectedTableCards] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [matchChoiceError, setMatchChoiceError] = useState<string | null>(null);
  const [dismissedRoundIds, setDismissedRoundIds] = useState<Set<string>>(() => new Set());
  const [autoDealKey, setAutoDealKey] = useState<string | null>(null);
  const [captureAnimation, setCaptureAnimation] = useState<CaptureAnimation | null>(null);
  const previousRoundRef = useRef<{ roundId: string; gameState: GameState } | null>(null);

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

  useEffect(() => {
    if (!match?.id || match.status === 'ABANDONED') {
      return;
    }

    const intervalId = window.setInterval(async () => {
      try {
        const refreshedMatch = await getMatch(match.id);
        setMatch(refreshedMatch);
      } catch {
        // Keep the last playable state on transient polling failures.
      }
    }, 3000);

    return () => window.clearInterval(intervalId);
  }, [match?.id, match?.status]);

  useEffect(() => {
    if (match?.rematchId) {
      navigate(`/game/${match.rematchId}`, { replace: true });
    }
  }, [match?.rematchId, navigate]);

  useEffect(() => {
    const dealKey = match ? `${match.currentRound.id}:${match.currentRound.gameState.deckCount}` : null;
    if (!shouldAutoDeal(match) || autoDealKey === dealKey) {
      return;
    }

    let cancelled = false;

    async function dealHand() {
      setAutoDealKey(dealKey);
      const dealtMatch = await run(() => dealMatchRound(match!.id, match!.currentRound.id), { suppressAutoDealConflict: true });
      if (!cancelled && dealtMatch && dealtMatch.currentRound.gameState.deckCount !== match!.currentRound.gameState.deckCount) {
        setSelectedTableCards([]);
        setAutoDealKey(null);
      }
    }

    dealHand();

    return () => {
      cancelled = true;
    };
  }, [
    match?.id,
    match?.currentRound.id,
    match?.currentRound.gameState.deckCount,
    match?.currentRound.gameState.myHand.length,
    match?.currentRound.gameState.opponentHandCount,
    match?.currentRound.gameState.opponentHand.length,
    match?.currentRound.gameState.pendingCaptureCard?.id,
    match?.currentRound.gameState.phase,
    match?.status,
    autoDealKey
  ]);

  useEffect(() => {
    if (!match) {
      previousRoundRef.current = null;
      return;
    }

    const currentRound = match.currentRound;
    const previousRound = previousRoundRef.current;
    if (captureAnimationEnabled && previousRound?.roundId === currentRound.id) {
      const animation = capturedCardsAnimation(previousRound.gameState, currentRound.gameState);
      if (animation) {
        setCaptureAnimation(animation);
      }
    }

    previousRoundRef.current = {
      roundId: currentRound.id,
      gameState: currentRound.gameState
    };
  }, [captureAnimationEnabled, match]);

  useEffect(() => {
    if (!captureAnimationEnabled) {
      setCaptureAnimation(null);
    }
  }, [captureAnimationEnabled]);

  useEffect(() => {
    if (!captureAnimation) {
      return;
    }

    const timeoutId = window.setTimeout(() => setCaptureAnimation(null), 1800);
    return () => window.clearTimeout(timeoutId);
  }, [captureAnimation]);

  useEffect(() => {
    const game = match?.currentRound.gameState;
    if (game?.pendingCapturePlayer !== 'ME' || game.pendingCaptureCard?.rank !== 'JACK') {
      return;
    }

    const jackCaptureIds = game.tableCards
      .filter((card) => card.id !== game.pendingCaptureCard?.id && isJackCapturable(card))
      .map((card) => card.id);

    setSelectedTableCards((current) => arraysEqual(current, jackCaptureIds) ? current : jackCaptureIds);
  }, [
    match?.currentRound.id,
    match?.currentRound.gameState.pendingCapturePlayer,
    match?.currentRound.gameState.pendingCaptureCard?.id,
    match?.currentRound.gameState.pendingCaptureCard?.rank,
    match?.currentRound.gameState.tableCards
  ]);

  async function run(action: () => Promise<MatchState>, options: { suppressAutoDealConflict?: boolean } = {}) {
    try {
      setError(null);
      const nextMatch = await action();
      setMatch(nextMatch);
      return nextMatch;
    } catch (caught) {
      const message = caught instanceof Error ? caught.message : 'Something went wrong.';
      if (!options.suppressAutoDealConflict || !isAutoDealConflict(message)) {
        setError(message);
      }
      return null;
    }
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
    if (game.pendingCapturePlayer !== 'ME') {
      setError('Wait for your opponent to finish capturing.');
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
    if (!window.confirm('Are you sure you want to exit the match?')) {
      return;
    }
    const exitedMatch = await run(() => exitMatch(match.id));
    if (exitedMatch) {
      navigate('/dashboard');
    }
  }

  async function handleAcknowledgeRound(round: RoundState) {
    if (!match) {
      return;
    }
    setDismissedRoundIds((current) => new Set(current).add(round.id));
    try {
      setError(null);
      const nextMatch = await acknowledgeRound(match.id, round.id);
      setMatch(nextMatch);
    } catch {
      // The popup is already dismissed locally; stale deployments can miss the ack endpoint.
    }
  }

  async function handleMatchEndChoice(choice: MatchEndChoice) {
    if (!match) {
      return;
    }
    setMatchChoiceError(null);
    if (choice === 'DASHBOARD') {
      try {
        await chooseMatchEnd(match.id, choice);
      } catch {
        // Leaving the page is local navigation; backend persistence is best effort here.
      }
      navigate('/dashboard');
      return;
    }

    const nextMatch = await run(() => chooseMatchEnd(match.id, choice));
    if (!nextMatch) {
      return;
    }
    if (nextMatch.id !== match.id) {
      navigate(`/game/${nextMatch.id}`, { replace: true });
      return;
    }
    const opponentChoice = opponentMatchEndChoice(nextMatch);
    if (opponentChoice === 'DASHBOARD') {
      setMatchChoiceError('The other player has exited.');
    }
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
          <button type="button" onClick={() => navigate('/dashboard')}>
            Try again
          </button>
        </section>
      </main>
    );
  }

  if (!match) {
    return <main className="loading">Loading Pasoor...</main>;
  }

  if (match.status === 'WAITING') {
    return (
      <main className="app-shell">
        <section className="game-load-error waiting-panel">
          <p className="eyebrow">Pasoor</p>
          <h1>Waiting for friend</h1>
          <p className="muted-text">Your friend has been invited. The match starts when they accept.</p>
          {error && <p className="error-message">{error}</p>}
          <Link to="/friends">Friends</Link>
        </section>
      </main>
    );
  }

  if (match.status === 'ABANDONED') {
    const otherExited = match.exitedBy && !isViewerPlayer(match, match.exitedBy);
    if (otherExited) {
      return (
        <main className="app-shell">
          <StatusDialog
            title="Match exited"
            message={`${firstName(match.exitedBy?.name, 'The other player')} exited the match.`}
            actions={
              <button type="button" onClick={() => navigate('/dashboard')}>
                OK
              </button>
            }
          />
        </main>
      );
    }

    return (
      <main className="app-shell">
        <section className="game-load-error waiting-panel">
          <p className="eyebrow">Pasoor</p>
          <h1>Match ended</h1>
          <p className="error-message">
            {inviteToken ? 'Your friend denied the game invite.' : 'This match was exited.'}
          </p>
          <div className="profile-actions">
            <Link className="secondary-action-button" to="/dashboard">Dashboard</Link>
            <Link className="secondary-action-button" to="/friends">Friends</Link>
          </div>
        </section>
      </main>
    );
  }

  const playerNames = visiblePlayerNames(match);
  const roundForPopup = match.status === 'ACTIVE' || match.status === 'FINISHED'
    ? unacknowledgedRound(match, dismissedRoundIds)
    : null;
  const matchEndConflict = matchEndConflictMessage(match);

  return (
    <main className="app-shell">
      <GameBoard
        game={match.currentRound.gameState}
        match={match}
        selectedTableCards={selectedTableCards}
        error={error}
        onPlayCard={handlePlayCard}
        onToggleTableCard={handleToggleTableCard}
        onCapture={handleCapture}
        onExitMatch={handleExitMatch}
        captureAnimation={captureAnimation}
      />
      {roundForPopup?.gameState.score && (
        <StatusDialog
          title={`Game ${roundForPopup.roundNumber} score`}
          actions={
            <button type="button" onClick={() => handleAcknowledgeRound(roundForPopup)}>
              OK
            </button>
          }
        >
          <ScoreBoard
            score={roundForPopup.gameState.score}
            myName={playerNames.ME}
            opponentName={playerNames.OPPONENT}
            myCollectedPile={roundForPopup.gameState.myCollectedPile}
            opponentCollectedPile={roundForPopup.gameState.opponentCollectedPile}
          />
        </StatusDialog>
      )}
      {match.status === 'FINISHED' && !roundForPopup && (
        <StatusDialog
          title="Match ended"
          message={matchWinnerMessage(match)}
          showConfetti={match.winnerSide === match.viewerSide}
          actions={
            <>
              <button type="button" onClick={() => handleMatchEndChoice('PLAY_AGAIN')}>
                Play another match
              </button>
              <button type="button" className="secondary-action-button" onClick={() => handleMatchEndChoice('DASHBOARD')}>
                Go back to Dashboard
              </button>
            </>
          }
        >
          {(matchChoiceError || matchEndConflict) && <p className="error-message">{matchChoiceError ?? matchEndConflict}</p>}
          <div className="match-round-list">
            {(match.completedRounds ?? []).filter((round) => round.gameState.score).map((round) => (
              <ScoreBoard
                key={round.id}
                score={round.gameState.score!}
                myName={playerNames.ME}
                opponentName={playerNames.OPPONENT}
                myCollectedPile={round.gameState.myCollectedPile}
                opponentCollectedPile={round.gameState.opponentCollectedPile}
                title={`Game ${round.roundNumber}`}
              />
            ))}
            <div className="match-total-score">
              <span>Match total</span>
              <strong>{playerNames.ME} {visibleMatchTotals(match).myTotal} - {visibleMatchTotals(match).opponentTotal} {playerNames.OPPONENT}</strong>
            </div>
          </div>
        </StatusDialog>
      )}
    </main>
  );
}

function StatusDialog({
  title,
  message,
  actions,
  children,
  showConfetti = false
}: {
  title: string;
  message?: string;
  actions: ReactNode;
  children?: ReactNode;
  showConfetti?: boolean;
}) {
  return (
    <div className="invite-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="status-dialog-title">
      {showConfetti && <Confetti />}
      <section className="invite-modal status-dialog">
        <p className="eyebrow">Pasoor</p>
        <h1 id="status-dialog-title">{title}</h1>
        {message && <p className="muted-text">{message}</p>}
        {children}
        <div className="profile-actions">{actions}</div>
      </section>
    </div>
  );
}

function Confetti() {
  return (
    <div className="confetti" aria-hidden="true">
      {Array.from({ length: 24 }, (_, index) => (
        <span key={index} style={{ '--confetti-index': index } as CSSProperties} />
      ))}
    </div>
  );
}

function unacknowledgedRound(match: MatchState, dismissedRoundIds: Set<string>) {
  const round = match.lastCompletedRound;
  if (!round?.gameState.score) {
    return null;
  }
  if (dismissedRoundIds.has(round.id)) {
    return null;
  }
  if (viewerAcknowledged(match, round)) {
    return null;
  }

  return round;
}

function shouldAutoDeal(match: MatchState | null) {
  if (!match || match.status !== 'ACTIVE') {
    return false;
  }

  const game = match.currentRound.gameState;
  const opponentHandCount = game.opponentHandCount ?? game.opponentHand.length;
  const bothHandsEmpty = game.myHand.length === 0 && opponentHandCount === 0;

  return game.phase === 'NEW'
    || (game.phase === 'PLAYING' && game.deckCount > 0 && bothHandsEmpty && game.pendingCaptureCard === null);
}

function isAutoDealConflict(message: string) {
  return message.includes('Deal only when both hands are empty')
    || message.includes('Round not found')
    || message.includes('Match is not active');
}

function viewerAcknowledged(match: MatchState, round: RoundState) {
  return match.viewerSide === 'PLAYER_ONE' ? round.playerOneAcknowledged : round.playerTwoAcknowledged;
}

function opponentMatchEndChoice(match: MatchState) {
  return match.viewerSide === 'PLAYER_ONE' ? match.playerTwoEndChoice : match.playerOneEndChoice;
}

function viewerMatchEndChoice(match: MatchState) {
  return match.viewerSide === 'PLAYER_ONE' ? match.playerOneEndChoice : match.playerTwoEndChoice;
}

function matchEndConflictMessage(match: MatchState) {
  if (viewerMatchEndChoice(match) === 'PLAY_AGAIN' && opponentMatchEndChoice(match) === 'DASHBOARD') {
    return 'The other player has exited.';
  }

  return null;
}

function isViewerPlayer(match: MatchState, player: MatchPlayer) {
  return match.viewerSide === 'PLAYER_ONE'
    ? match.playerOne.id === player.id
    : match.playerTwo?.id === player.id;
}

function visiblePlayerNames(match: MatchState): Record<Player, string> {
  if (match.viewerSide === 'PLAYER_TWO') {
    return {
      ME: firstName(match.playerTwo?.name, 'You'),
      OPPONENT: firstName(match.playerOne.name, 'Opponent')
    };
  }

  return {
    ME: firstName(match.playerOne.name, 'You'),
    OPPONENT: firstName(match.playerTwo?.name, 'Opponent')
  };
}

function firstName(name: string | null | undefined, fallback: string) {
  const trimmed = name?.trim();
  return trimmed ? trimmed.split(/\s+/)[0] : fallback;
}

function matchWinnerMessage(match: MatchState) {
  if (match.winnerSide === match.viewerSide) {
    return 'You won the match.';
  }
  if (match.winner) {
    return `${firstName(match.winner.name, 'The other player')} won the match.`;
  }

  return 'The match is complete.';
}

function visibleRoundScore(match: MatchState, round: RoundState) {
  const myScore = match.viewerSide === 'PLAYER_TWO' ? round.playerTwoRoundScore : round.playerOneRoundScore;
  const opponentScore = match.viewerSide === 'PLAYER_TWO' ? round.playerOneRoundScore : round.playerTwoRoundScore;
  const names = visiblePlayerNames(match);

  return `${names.ME} ${myScore ?? 0} - ${opponentScore ?? 0} ${names.OPPONENT}`;
}

function visibleMatchTotals(match: MatchState) {
  if (match.viewerSide === 'PLAYER_TWO') {
    return {
      myTotal: match.playerTwoTotalScore,
      opponentTotal: match.playerOneTotalScore
    };
  }

  return {
    myTotal: match.playerOneTotalScore,
    opponentTotal: match.playerTwoTotalScore
  };
}

function capturedCardsAnimation(previous: GameState, next: GameState): CaptureAnimation | null {
  if (!previous.pendingCaptureCard || previous.pendingCapturePlayer === null || next.pendingCaptureCard !== null) {
    return null;
  }

  const previousTableIds = new Set(previous.tableCards.map((card) => card.id));
  const nextTableIds = new Set(next.tableCards.map((card) => card.id));
  const removedCards = previous.tableCards.filter((card) => previousTableIds.has(card.id) && !nextTableIds.has(card.id));
  const capturedCards = uniqueCards([previous.pendingCaptureCard, ...removedCards]);

  if (capturedCards.length <= 1) {
    return null;
  }

  return {
    id: `${previous.pendingCaptureCard.id}:${next.myCollectedPile.length}:${next.opponentCollectedPile.length}`,
    cards: capturedCards,
    player: previous.pendingCapturePlayer
  };
}

function uniqueCards(cards: Card[]) {
  const seen = new Set<string>();
  return cards.filter((card) => {
    if (seen.has(card.id)) {
      return false;
    }
    seen.add(card.id);
    return true;
  });
}

function isJackCapturable(card: Card) {
  return card.rank === 'JACK' || (card.rank !== 'QUEEN' && card.rank !== 'KING');
}

function arraysEqual(left: string[], right: string[]) {
  return left.length === right.length && left.every((value, index) => value === right[index]);
}
