import type { GameState } from './game';

export type MatchStatus = 'WAITING' | 'ACTIVE' | 'FINISHED' | 'ABANDONED';
export type RoundStatus = 'ACTIVE' | 'FINISHED';
export type MatchWinner = 'PLAYER_ONE' | 'PLAYER_TWO';
export type MatchPlayerSide = 'PLAYER_ONE' | 'PLAYER_TWO';
export type MatchEndChoice = 'PLAY_AGAIN' | 'DASHBOARD';

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
  playerOneAcknowledged: boolean;
  playerTwoAcknowledged: boolean;
}

export interface MatchState {
  id: string;
  status: MatchStatus;
  playerOne: MatchPlayer;
  playerTwo: MatchPlayer | null;
  viewerSide: MatchPlayerSide;
  playerOneTotalScore: number;
  playerTwoTotalScore: number;
  winnerSide: MatchWinner | null;
  winner: MatchPlayer | null;
  exitedBy: MatchPlayer | null;
  playerOneEndChoice: MatchEndChoice | null;
  playerTwoEndChoice: MatchEndChoice | null;
  rematchId: string | null;
  currentRound: RoundState;
  lastCompletedRound: RoundState | null;
  completedRounds: RoundState[];
}
