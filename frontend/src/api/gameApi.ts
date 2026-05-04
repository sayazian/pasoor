import type { GameState, Player } from '../types/game';

const API_BASE = 'http://localhost:8080/api/game';

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers
    },
    ...options
  });

  if (!response.ok) {
    throw new Error(await response.text());
  }

  return response.json();
}

export function newGame(): Promise<GameState> {
  return request<GameState>('/new', { method: 'POST' });
}

export function dealCards(): Promise<GameState> {
  return request<GameState>('/deal', { method: 'POST' });
}

export function playCard(player: Player, cardId: string): Promise<GameState> {
  return request<GameState>('/play', {
    method: 'POST',
    body: JSON.stringify({ player, cardId })
  });
}

export function captureCards(player: Player, capturedTableCardIds: string[]): Promise<GameState> {
  return request<GameState>('/capture', {
    method: 'POST',
    body: JSON.stringify({ player, capturedTableCardIds })
  });
}
