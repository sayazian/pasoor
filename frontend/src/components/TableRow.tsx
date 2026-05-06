import type { Card as CardType, Player } from '../types/game';
import Card from './Card';

interface TableRowProps {
  cards: CardType[];
  selectedCardIds: string[];
  pendingCaptureCardId: string | null;
  pendingCapturePlayer: Player | null;
  prompt: string;
  onToggleCard: (cardId: string) => void;
  onCapture: () => void;
}

export default function TableRow({
  cards,
  selectedCardIds,
  pendingCaptureCardId,
  pendingCapturePlayer,
  prompt,
  onToggleCard,
  onCapture
}: TableRowProps) {
  const hasPendingCapture = pendingCaptureCardId !== null;
  const canChooseCapture = hasPendingCapture && pendingCapturePlayer === 'ME';

  return (
    <section className="table-row">
      <div className="row-heading">
        <h2>Table</h2>
        {canChooseCapture ? (
          <div className="collect-actions">
            <span>{prompt}</span>
            <span>{selectedCardIds.length} selected</span>
            <button type="button" onClick={onCapture}>
              Capture
            </button>
          </div>
        ) : (
          <span>{prompt}</span>
        )}
      </div>
      <div className="cards-row table-cards">
        {cards.length === 0 ? (
          <p className="empty-label">Empty</p>
        ) : (
          cards.map((card) => (
            <Card
              key={card.id}
              card={card}
              selected={selectedCardIds.includes(card.id) || card.id === pendingCaptureCardId}
              disabled={!canChooseCapture || card.id === pendingCaptureCardId}
              onClick={() => onToggleCard(card.id)}
            />
          ))
        )}
      </div>
    </section>
  );
}
