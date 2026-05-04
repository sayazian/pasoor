import '@testing-library/jest-dom/vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import type { GameState } from './types/game';

const currentUser = {
  id: 'e51b2b5a-5797-40be-8771-caa189a2f8bd',
  name: 'Sahar',
  email: 'sahar@example.com',
  avatarUrl: null,
  preferredTheme: 'CLASSIC_GREEN_FELT'
};

const newGameState: GameState = {
  deck: [],
  deckCount: 52,
  myHand: [],
  opponentHand: [],
  tableCards: [],
  myCollectedPile: [],
  opponentCollectedPile: [],
  currentTurn: 'ME',
  phase: 'NEW',
  initialDealDone: false,
  mySurCount: 0,
  opponentSurCount: 0,
  pendingCapturePlayer: null,
  pendingCaptureCard: null,
  lastCapturePlayer: null,
  score: null
};

describe('App routing', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    window.history.pushState({}, '', '/');
  });

  afterEach(() => {
    cleanup();
  });

  it('loads the login route directly', async () => {
    window.history.pushState({}, '', '/login');
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 401 }));

    render(<App />);

    expect(await screen.findByRole('heading', { name: /sign in/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /continue with google/i })).toHaveAttribute(
      'href',
      'http://localhost:8080/oauth2/authorization/google'
    );
  });

  it('loads the dashboard route directly when the session is authenticated', async () => {
    window.history.pushState({}, '', '/dashboard');
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(Response.json(currentUser));

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Sahar' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /create game/i })).toHaveAttribute('href', '/game');
    expect(screen.getByRole('button', { name: /log out/i })).toBeInTheDocument();
  });

  it('redirects protected routes to login for anonymous sessions', async () => {
    window.history.pushState({}, '', '/dashboard');
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 401 }));

    render(<App />);

    expect(await screen.findByRole('heading', { name: /sign in/i })).toBeInTheDocument();
  });

  it('loads the game route directly when the session is authenticated', async () => {
    window.history.pushState({}, '', '/game');
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/game/new')) {
        return Response.json(newGameState);
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByRole('button', { name: /new game/i })).toBeInTheDocument();
    expect(screen.getByText('My turn')).toBeInTheDocument();
  });

  it('logs out from the dashboard and returns to login', async () => {
    window.history.pushState({}, '', '/dashboard');
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/logout') && init?.method === 'POST') {
        return new Response(null, { status: 204 });
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    fireEvent.click(await screen.findByRole('button', { name: /log out/i }));

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith('http://localhost:8080/api/logout', {
        method: 'POST',
        credentials: 'include'
      });
    });
    expect(await screen.findByRole('heading', { name: /sign in/i })).toBeInTheDocument();
  });

  it('waits for the session before choosing the root route', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(Response.json(currentUser));

    render(<App />);

    expect(await screen.findByRole('heading', { name: 'Sahar' })).toBeInTheDocument();
  });
});
