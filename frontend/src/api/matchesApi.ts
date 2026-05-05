import type { Player } from '../types/game';
import type { GameInvite, GameInviteListResponse } from '../types/invite';
import type { MatchState } from '../types/match';
import { request } from './http';

export function createMatch(): Promise<MatchState> {
  return request<MatchState>('/api/matches', { method: 'POST' });
}

export function getMatch(matchId: string): Promise<MatchState> {
  return request<MatchState>(`/api/matches/${matchId}`);
}

export function exitMatch(matchId: string): Promise<MatchState> {
  return request<MatchState>(`/api/matches/${matchId}/exit`, { method: 'POST' });
}

export function inviteFriendToMatch(matchId: string, email: string): Promise<GameInvite> {
  return request<GameInvite>(`/api/matches/${matchId}/invite`, {
    method: 'POST',
    body: JSON.stringify({ email })
  });
}

export function getInvite(token: string): Promise<GameInvite> {
  return request<GameInvite>(`/api/invites/${token}`);
}

export function getLiveInvites(): Promise<GameInviteListResponse> {
  return request<GameInviteListResponse>('/api/invites');
}

export function acceptInvite(token: string): Promise<GameInvite> {
  return request<GameInvite>(`/api/invites/${token}/accept`, { method: 'POST' });
}

export function declineInvite(token: string): Promise<GameInvite> {
  return request<GameInvite>(`/api/invites/${token}/decline`, { method: 'POST' });
}

export function dealMatchRound(matchId: string, roundId: string): Promise<MatchState> {
  return request<MatchState>(`/api/matches/${matchId}/rounds/${roundId}/deal`, { method: 'POST' });
}

export function playMatchCard(matchId: string, roundId: string, player: Player, cardId: string): Promise<MatchState> {
  return request<MatchState>(`/api/matches/${matchId}/rounds/${roundId}/play`, {
    method: 'POST',
    body: JSON.stringify({ player, cardId })
  });
}

export function captureMatchCards(
  matchId: string,
  roundId: string,
  player: Player,
  capturedTableCardIds: string[]
): Promise<MatchState> {
  return request<MatchState>(`/api/matches/${matchId}/rounds/${roundId}/capture`, {
    method: 'POST',
    body: JSON.stringify({ player, capturedTableCardIds })
  });
}
