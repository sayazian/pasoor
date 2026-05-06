import type { GamePhase } from '../types/game';
import Card from './Card';

interface DeckPileProps {
  count: number;
  phase: GamePhase;
  canDeal?: boolean;
  onDeal?: () => void;
}

export default function DeckPile({ count, phase, canDeal = false, onDeal }: DeckPileProps) {
  const disabled = !canDeal || count === 0 || phase === 'FINISHED';

  return (
    <div className="pile-panel">
      <p className="panel-title">Deck</p>
      <div className={disabled ? 'deck-wrap deck-disabled' : 'deck-wrap'}>
        <Card faceDown disabled={disabled} onClick={onDeal} />
        <span className="deck-count">{count}</span>
      </div>
    </div>
  );
}
