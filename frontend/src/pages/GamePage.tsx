import { useEffect, useState } from 'react';
import { captureCards, dealCards, newGame, playCard } from '../api/gameApi';
import GameBoard from '../components/GameBoard';
import type { GameState, Player } from '../types/game';

export default function GamePage() {
  const [game, setGame] = useState<GameState | null>(null);
  const [selectedTableCards, setSelectedTableCards] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    handleNewGame();
  }, []);

  async function run(action: () => Promise<GameState>) {
    try {
      setError(null);
      const nextGame = await action();
      setGame(nextGame);
      return nextGame;
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Something went wrong.');
      return null;
    }
  }

  async function handleNewGame() {
    setSelectedTableCards([]);
    await run(newGame);
  }

  async function handleDeal() {
    setSelectedTableCards([]);
    await run(dealCards);
  }

  async function handlePlayCard(player: Player, cardId: string) {
    const nextGame = await run(() => playCard(player, cardId));
    if (nextGame) {
      setSelectedTableCards([]);
    }
  }

  async function handleCapture() {
    if (!game?.pendingCapturePlayer) {
      setError('Drop a card before capturing.');
      return;
    }
    if (selectedTableCards.length === 0) {
      setError('Select at least one table card to capture.');
      return;
    }

    const nextGame = await run(() => captureCards(game.pendingCapturePlayer!, selectedTableCards));
    if (nextGame) {
      setSelectedTableCards([]);
    }
  }

  function handleToggleTableCard(cardId: string) {
    setSelectedTableCards((current) =>
      current.includes(cardId) ? current.filter((id) => id !== cardId) : [...current, cardId]
    );
  }

  if (!game) {
    return <main className="loading">Loading Pasoor...</main>;
  }

  return (
    <main className="app-shell">
      <GameBoard
        game={game}
        selectedTableCards={selectedTableCards}
        error={error}
        onDeal={handleDeal}
        onPlayCard={handlePlayCard}
        onToggleTableCard={handleToggleTableCard}
        onCapture={handleCapture}
        onNewGame={handleNewGame}
      />
    </main>
  );
}

