import type { Card as CardType, Player } from '../types/game';
import Card from './Card';

interface TableRowProps {
  cards: CardType[];
  selectedCardIds: string[];
  onToggleCard: (cardId: string) => void;
  onCollect: (player: Player) => void;
}

export default function TableRow({ cards, selectedCardIds, onToggleCard, onCollect }: TableRowProps) {
  return (
    <section className="table-row">
      <div className="row-heading">
        <h2>Table</h2>
        <div className="collect-actions">
          <button type="button" onClick={() => onCollect('OPPONENT')}>
            To opponent
          </button>
          <button type="button" onClick={() => onCollect('ME')}>
            To me
          </button>
        </div>
      </div>
      <div className="cards-row table-cards">
        {cards.length === 0 ? (
          <p className="empty-label">Empty</p>
        ) : (
          cards.map((card) => (
            <Card
              key={card.id}
              card={card}
              selected={selectedCardIds.includes(card.id)}
              onClick={() => onToggleCard(card.id)}
            />
          ))
        )}
      </div>
    </section>
  );
}
