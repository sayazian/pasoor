import type { Card as CardType, Player } from '../types/game';
import Card from './Card';

interface HandRowProps {
  title: string;
  player: Player;
  cards: CardType[];
  cardCount?: number;
  currentTurn: Player;
  faceDown?: boolean;
  disabled?: boolean;
  onPlayCard?: (player: Player, cardId: string) => void;
}

export default function HandRow({ title, player, cards, cardCount, currentTurn, faceDown, disabled, onPlayCard }: HandRowProps) {
  const visibleCount = cardCount ?? cards.length;
  const isTurn = currentTurn === player && !disabled && Boolean(onPlayCard);

  return (
    <section className="hand-row">
      <div className="row-heading">
        <h2>{title}</h2>
        <span>{visibleCount} cards</span>
      </div>
      <div className="cards-row">
        {visibleCount === 0 ? (
          <p className="empty-label">Empty</p>
        ) : faceDown ? (
          Array.from({ length: visibleCount }, (_, index) => (
            <Card key={`${title}-${index}`} faceDown disabled />
          ))
        ) : (
          cards.map((card) => (
            <Card
              key={card.id}
              card={card}
              faceDown={faceDown}
              disabled={!isTurn}
              onClick={() => onPlayCard?.(player, card.id)}
            />
          ))
        )}
      </div>
    </section>
  );
}
