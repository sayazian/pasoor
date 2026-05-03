import Card from './Card';

interface CollectedPileProps {
  title: string;
  count: number;
}

export default function CollectedPile({ title, count }: CollectedPileProps) {
  return (
    <div className="pile-panel collected-panel">
      <p className="panel-title">{title}</p>
      <div className="stacked-pile">
        <Card faceDown disabled />
        <span className="pile-count">{count}</span>
      </div>
    </div>
  );
}
