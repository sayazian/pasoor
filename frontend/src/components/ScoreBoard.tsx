import type { Score } from '../types/game';

interface ScoreBoardProps {
  score: Score;
}

export default function ScoreBoard({ score }: ScoreBoardProps) {
  return (
    <section className="score-board">
      <div>
        <p className="panel-title">Final score</p>
        <h2>
          Me {score.myScore} - {score.opponentScore} Opponent
        </h2>
      </div>
      <div className="score-grid">
        <span>Clubs</span>
        <strong>{score.myClubCount}</strong>
        <strong>{score.opponentClubCount}</strong>
        <span>Card points</span>
        <strong>{score.myCardPoints}</strong>
        <strong>{score.opponentCardPoints}</strong>
        <span>Sur points</span>
        <strong>{score.mySurPoints}</strong>
        <strong>{score.opponentSurPoints}</strong>
      </div>
    </section>
  );
}
