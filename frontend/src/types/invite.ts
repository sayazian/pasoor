import type { MatchPlayer, MatchState } from './match';

export type GameInviteStatus = 'PENDING' | 'ACCEPTED' | 'EXPIRED';

export interface GameInvite {
  id: string;
  status: GameInviteStatus;
  sender: MatchPlayer;
  recipient: MatchPlayer;
  recipientEmail: string;
  token: string;
  inviteLink: string;
  match: MatchState;
}
