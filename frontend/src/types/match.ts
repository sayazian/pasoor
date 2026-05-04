import type { GameState } from './game';

export type MatchStatus = 'WAITING' | 'ACTIVE' | 'FINISHED' | 'ABANDONED';
export type RoundStatus = 'ACTIVE' | 'FINISHED';
export type MatchWinner = 'PLAYER_ONE' | 'PLAYER_TWO';

export interface MatchPlayer {
  id: string;
  name: string;
  email: string;
  avatarUrl: string | null;
}

export interface RoundState {
  id: string;
  roundNumber: number;
  status: RoundStatus;
  gameState: GameState;
  playerOneRoundScore: number | null;
  playerTwoRoundScore: number | null;
}

export interface MatchState {
  id: string;
  status: MatchStatus;
  playerOne: MatchPlayer;
  playerTwo: MatchPlayer | null;
  playerOneTotalScore: number;
  playerTwoTotalScore: number;
  winnerSide: MatchWinner | null;
  winner: MatchPlayer | null;
  currentRound: RoundState;
}
