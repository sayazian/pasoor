import type { Card as CardType } from '../types/game';

interface CardProps {
  card?: CardType;
  faceDown?: boolean;
  selected?: boolean;
  disabled?: boolean;
  onClick?: () => void;
}

const suitSymbols = {
  CLUBS: '♣',
  DIAMONDS: '♦',
  HEARTS: '♥',
  SPADES: '♠'
} as const;

const rankLabels = {
  ACE: 'A',
  TWO: '2',
  THREE: '3',
  FOUR: '4',
  FIVE: '5',
  SIX: '6',
  SEVEN: '7',
  EIGHT: '8',
  NINE: '9',
  TEN: '10',
  JACK: 'J',
  QUEEN: 'Q',
  KING: 'K'
} as const;

export default function Card({ card, faceDown, selected, disabled, onClick }: CardProps) {
  const isRed = card?.suit === 'DIAMONDS' || card?.suit === 'HEARTS';

  return (
    <button
      className={[
        'card',
        faceDown ? 'card-back' : '',
        selected ? 'selected' : '',
        disabled ? 'disabled' : '',
        isRed ? 'red-card' : ''
      ].join(' ')}
      type="button"
      disabled={disabled}
      onClick={onClick}
      aria-label={faceDown || !card ? 'Face-down card' : `${rankLabels[card.rank]} of ${card.suit.toLowerCase()}`}
    >
      {faceDown || !card ? null : (
        <>
          <span>{rankLabels[card.rank]}</span>
          <strong>{suitSymbols[card.suit]}</strong>
          <span>{rankLabels[card.rank]}</span>
        </>
      )}
    </button>
  );
}
