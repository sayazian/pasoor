import type { GameState, Player } from '../types/game';
import { request } from './http';

export function newGame(): Promise<GameState> {
  return request<GameState>('/api/game/new', { method: 'POST' });
}

export function dealCards(): Promise<GameState> {
  return request<GameState>('/api/game/deal', { method: 'POST' });
}

export function playCard(player: Player, cardId: string): Promise<GameState> {
  return request<GameState>('/api/game/play', {
    method: 'POST',
    body: JSON.stringify({ player, cardId })
  });
}

export function captureCards(player: Player, capturedTableCardIds: string[]): Promise<GameState> {
  return request<GameState>('/api/game/capture', {
    method: 'POST',
    body: JSON.stringify({ player, capturedTableCardIds })
  });
}
