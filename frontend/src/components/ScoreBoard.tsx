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
        <span>Jacks</span>
        <strong>{score.myJackCount}</strong>
        <strong>{score.opponentJackCount}</strong>
        <span>Aces</span>
        <strong>{score.myAceCount}</strong>
        <strong>{score.opponentAceCount}</strong>
        <span>10 of diamonds</span>
        <strong>{score.myTenOfDiamondsCount}</strong>
        <strong>{score.opponentTenOfDiamondsCount}</strong>
        <span>2 of clubs</span>
        <strong>{score.myTwoOfClubsCount}</strong>
        <strong>{score.opponentTwoOfClubsCount}</strong>
      </div>
    </section>
  );
}
