import { useEffect, useState } from 'react';
import { collectCards, dealCards, newGame, playCard } from './api/gameApi';
import GameBoard from './components/GameBoard';
import type { GameState, Player } from './types/game';

export default function App() {
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
    await run(() => playCard(player, cardId));
  }

  function handleToggleTableCard(cardId: string) {
    setSelectedTableCards((current) =>
      current.includes(cardId) ? current.filter((id) => id !== cardId) : [...current, cardId]
    );
  }

  async function handleCollect(player: Player) {
    if (selectedTableCards.length === 0) {
      setError('Select at least one table card first.');
      return;
    }

    const nextGame = await run(() => collectCards(player, selectedTableCards));
    if (nextGame) {
      setSelectedTableCards([]);
    }
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
        onCollect={handleCollect}
        onNewGame={handleNewGame}
      />
    </main>
  );
}
