export type Suit = 'CLUBS' | 'DIAMONDS' | 'HEARTS' | 'SPADES';
export type Rank =
  | 'ACE'
  | 'TWO'
  | 'THREE'
  | 'FOUR'
  | 'FIVE'
  | 'SIX'
  | 'SEVEN'
  | 'EIGHT'
  | 'NINE'
  | 'TEN'
  | 'JACK'
  | 'QUEEN'
  | 'KING';
export type Player = 'ME' | 'OPPONENT';
export type GamePhase = 'NEW' | 'PLAYING' | 'FINISHED';

export interface Card {
  id: string;
  suit: Suit;
  rank: Rank;
  value: number;
}

export interface GameState {
  deck: Card[];
  deckCount: number;
  myHand: Card[];
  opponentHand: Card[];
  opponentHandCount?: number;
  tableCards: Card[];
  myCollectedPile: Card[];
  opponentCollectedPile: Card[];
  currentTurn: Player;
  phase: GamePhase;
  initialDealDone: boolean;
  mySurCount: number;
  opponentSurCount: number;
  pendingCapturePlayer: Player | null;
  pendingCaptureCard: Card | null;
  lastCapturePlayer: Player | null;
  score: Score | null;
}

export interface Score {
  myScore: number;
  opponentScore: number;
  myClubCount: number;
  opponentClubCount: number;
  mySurPoints: number;
  opponentSurPoints: number;
  myCardPoints: number;
  opponentCardPoints: number;
}
