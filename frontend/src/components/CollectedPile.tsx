import Card from './Card';

interface CollectedPileProps {
  title: string;
  count: number;
  surCount: number;
}

export default function CollectedPile({ title, count, surCount }: CollectedPileProps) {
  return (
    <div className="pile-panel collected-panel">
      <p className="panel-title">{title}</p>
      <div className="stacked-pile">
        <Card faceDown disabled />
        <span className="pile-count">{count}</span>
      </div>
      <p className="sur-count">Sur {surCount}</p>
    </div>
  );
}
