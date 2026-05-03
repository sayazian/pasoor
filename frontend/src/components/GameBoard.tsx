import { RotateCcw } from 'lucide-react';
import type { GameState, Player } from '../types/game';
import CollectedPile from './CollectedPile';
import DeckPile from './DeckPile';
import HandRow from './HandRow';
import TableRow from './TableRow';

interface GameBoardProps {
  game: GameState;
  selectedTableCards: string[];
  error: string | null;
  onDeal: () => void;
  onPlayCard: (player: Player, cardId: string) => void;
  onToggleTableCard: (cardId: string) => void;
  onCollect: (player: Player) => void;
  onNewGame: () => void;
}

export default function GameBoard({
  game,
  selectedTableCards,
  error,
  onDeal,
  onPlayCard,
  onToggleTableCard,
  onCollect,
  onNewGame
}: GameBoardProps) {
  return (
    <section className="game">
      <header className="status-bar">
        <div>
          <p className="eyebrow">Pasoor</p>
          <h1>{game.phase === 'FINISHED' ? 'Game finished' : `${labelForPlayer(game.currentTurn)} turn`}</h1>
        </div>
      </header>

      <div className="board">
        <aside className="left-column">
          <DeckPile count={game.deckCount} phase={game.phase} onDeal={onDeal} />
        </aside>

        <div className="middle-column">
          <HandRow
            title="Opponent"
            player="OPPONENT"
            cards={game.opponentHand}
            currentTurn={game.currentTurn}
            faceDown
            onPlayCard={onPlayCard}
          />
          <TableRow
            cards={game.tableCards}
            selectedCardIds={selectedTableCards}
            onToggleCard={onToggleTableCard}
            onCollect={onCollect}
          />
          <HandRow
            title="Me"
            player="ME"
            cards={game.myHand}
            currentTurn={game.currentTurn}
            onPlayCard={onPlayCard}
          />
        </div>

        <aside className="right-column">
          <CollectedPile title="Opponent used" count={game.opponentCollectedPile.length} />
          <CollectedPile title="My used" count={game.myCollectedPile.length} />
        </aside>
      </div>

      {error && <p className="error-message">{error}</p>}

      <footer className="bottom-actions">
        <button className="new-game-button" type="button" onClick={onNewGame}>
          <RotateCcw size={18} aria-hidden="true" />
          New Game
        </button>
      </footer>
    </section>
  );
}

function labelForPlayer(player: Player) {
  return player === 'ME' ? 'My' : "Opponent's";
}
