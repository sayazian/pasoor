import type { Card, Rank, Score, Suit } from '../types/game';

interface ScoreBoardProps {
  score: Score;
  myName?: string;
  opponentName?: string;
  myCollectedPile?: Card[];
  opponentCollectedPile?: Card[];
  title?: string;
}

export default function ScoreBoard({
  score,
  myName = 'Me',
  opponentName = 'Opponent',
  myCollectedPile = [],
  opponentCollectedPile = [],
  title = 'Final score'
}: ScoreBoardProps) {
  const myJacks = score.myJackCount ?? rankCount(myCollectedPile, 'JACK');
  const opponentJacks = score.opponentJackCount ?? rankCount(opponentCollectedPile, 'JACK');
  const myAces = score.myAceCount ?? rankCount(myCollectedPile, 'ACE');
  const opponentAces = score.opponentAceCount ?? rankCount(opponentCollectedPile, 'ACE');
  const myTenOfDiamonds = score.myTenOfDiamondsCount ?? specificCardCount(myCollectedPile, 'DIAMONDS', 'TEN');
  const opponentTenOfDiamonds = score.opponentTenOfDiamondsCount ?? specificCardCount(opponentCollectedPile, 'DIAMONDS', 'TEN');
  const myTwoOfClubs = score.myTwoOfClubsCount ?? specificCardCount(myCollectedPile, 'CLUBS', 'TWO');
  const opponentTwoOfClubs = score.opponentTwoOfClubsCount ?? specificCardCount(opponentCollectedPile, 'CLUBS', 'TWO');

  return (
    <section className="score-board">
      <div>
        <p className="panel-title">{title}</p>
        <h2>
          {myName} {score.myScore} - {score.opponentScore} {opponentName}
        </h2>
      </div>
      <div className="score-grid" aria-label="Score breakdown">
        <span />
        <strong>{myName}</strong>
        <strong>{opponentName}</strong>
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
        <strong>{myJacks}</strong>
        <strong>{opponentJacks}</strong>
        <span>Aces</span>
        <strong>{myAces}</strong>
        <strong>{opponentAces}</strong>
        <span>10 of diamonds</span>
        <strong>{myTenOfDiamonds}</strong>
        <strong>{opponentTenOfDiamonds}</strong>
        <span>2 of clubs</span>
        <strong>{myTwoOfClubs}</strong>
        <strong>{opponentTwoOfClubs}</strong>
      </div>
    </section>
  );
}

function rankCount(cards: Card[], rank: Rank) {
  return cards.filter((card) => card.rank === rank).length;
}

function specificCardCount(cards: Card[], suit: Suit, rank: Rank) {
  return cards.filter((card) => card.suit === suit && card.rank === rank).length;
}
