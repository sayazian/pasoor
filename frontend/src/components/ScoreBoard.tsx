import type { Score } from '../types/game';

interface ScoreBoardProps {
  score: Score;
  myName?: string;
  opponentName?: string;
}

export default function ScoreBoard({ score, myName = 'Me', opponentName = 'Opponent' }: ScoreBoardProps) {
  return (
    <section className="score-board">
      <div>
        <p className="panel-title">Final score</p>
        <h2>
          {myName} {score.myScore} - {score.opponentScore} {opponentName}
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
