import { RotateCcw } from 'lucide-react';
import type { GameState, Player } from '../types/game';
import CollectedPile from './CollectedPile';
import DeckPile from './DeckPile';
import HandRow from './HandRow';
import ScoreBoard from './ScoreBoard';
import TableRow from './TableRow';

interface GameBoardProps {
  game: GameState;
  selectedTableCards: string[];
  error: string | null;
  onDeal: () => void;
  onPlayCard: (player: Player, cardId: string) => void;
  onToggleTableCard: (cardId: string) => void;
  onCapture: () => void;
  onNewGame: () => void;
}

export default function GameBoard({
  game,
  selectedTableCards,
  error,
  onDeal,
  onPlayCard,
  onToggleTableCard,
  onCapture,
  onNewGame
}: GameBoardProps) {
  const isPendingCapture = game.pendingCaptureCard !== null;

  return (
    <section className="game">
      <div className="board">
        <aside className="left-column">
          <div className="left-status">
            <div>
              <p className="eyebrow">Pasoor</p>
              <h1>{game.phase === 'FINISHED' ? 'Game finished' : `${labelForPlayer(game.currentTurn)} turn`}</h1>
            </div>
          </div>
          <DeckPile count={game.deckCount} phase={game.phase} onDeal={onDeal} />
          <button className="new-game-button" type="button" onClick={onNewGame}>
            <RotateCcw size={18} aria-hidden="true" />
            New Game
          </button>
        </aside>

        <div className="middle-column">
          <HandRow
            title="Opponent"
            player="OPPONENT"
            cards={game.opponentHand}
            currentTurn={game.currentTurn}
            disabled={isPendingCapture}
            onPlayCard={onPlayCard}
          />
          <TableRow
            cards={game.tableCards}
            selectedCardIds={selectedTableCards}
            pendingCaptureCardId={game.pendingCaptureCard?.id ?? null}
            pendingCapturePlayer={game.pendingCapturePlayer}
            onToggleCard={onToggleTableCard}
            onCapture={onCapture}
          />
          <HandRow
            title="Me"
            player="ME"
            cards={game.myHand}
            currentTurn={game.currentTurn}
            disabled={isPendingCapture}
            onPlayCard={onPlayCard}
          />
        </div>

        <aside className="right-column">
          <CollectedPile title="Opponent used" count={game.opponentCollectedPile.length} surCount={game.opponentSurCount} />
          <CollectedPile title="My used" count={game.myCollectedPile.length} surCount={game.mySurCount} />
        </aside>
      </div>

      {error && <p className="error-message">{error}</p>}

      {game.phase === 'FINISHED' && game.score && <ScoreBoard score={game.score} />}

    </section>
  );
}

function labelForPlayer(player: Player) {
  return player === 'ME' ? 'My' : "Opponent's";
}
