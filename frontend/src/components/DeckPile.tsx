import type { GamePhase } from '../types/game';
import Card from './Card';

interface DeckPileProps {
  count: number;
  phase: GamePhase;
  onDeal: () => void;
}

export default function DeckPile({ count, phase, onDeal }: DeckPileProps) {
  const disabled = count === 0 || phase === 'FINISHED';

  return (
    <div className="pile-panel">
      <p className="panel-title">Deck</p>
      <div className="deck-wrap">
        <Card faceDown disabled={disabled} onClick={onDeal} />
        <span className="deck-count">{count}</span>
      </div>
    </div>
  );
}
