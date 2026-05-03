import type { Card as CardType, Player } from '../types/game';
import Card from './Card';

interface HandRowProps {
  title: string;
  player: Player;
  cards: CardType[];
  currentTurn: Player;
  faceDown?: boolean;
  onPlayCard: (player: Player, cardId: string) => void;
}

export default function HandRow({ title, player, cards, currentTurn, faceDown, onPlayCard }: HandRowProps) {
  const isTurn = currentTurn === player;

  return (
    <section className="hand-row">
      <div className="row-heading">
        <h2>{title}</h2>
        <span>{cards.length} cards</span>
      </div>
      <div className="cards-row">
        {cards.length === 0 ? (
          <p className="empty-label">Empty</p>
        ) : (
          cards.map((card) => (
            <Card
              key={card.id}
              card={card}
              faceDown={faceDown}
              disabled={!isTurn}
              onClick={() => onPlayCard(player, card.id)}
            />
          ))
        )}
      </div>
    </section>
  );
}
