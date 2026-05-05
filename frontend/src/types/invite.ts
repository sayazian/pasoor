import type { MatchPlayer, MatchState } from './match';

export type GameInviteStatus = 'INVITED' | 'ACCEPTED' | 'DECLINED';

export interface GameInvite {
  id: string;
  status: GameInviteStatus;
  sender: MatchPlayer;
  recipient: MatchPlayer;
  recipientEmail: string;
  token: string;
  match: MatchState;
}

export interface GameInviteListResponse {
  liveInvites: GameInvite[];
}
