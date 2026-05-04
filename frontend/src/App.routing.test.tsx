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

const emptyFriends = {
  friends: [],
  incomingRequests: [],
  outgoingRequests: []
};

const friend = {
  id: '1f5da4d5-9bd1-4968-8c3b-466b27940f5a',
  name: 'Friend',
  email: 'friend@example.com',
  avatarUrl: null
};

const incomingRequest = {
  id: '277fbb47-e518-40d9-859d-cdbd5efc532b',
  requester: friend,
  recipient: currentUser,
  status: 'PENDING',
  message: 'Want to play Pasoor?'
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

    const { container } = render(<App />);

    expect(await screen.findByRole('heading', { name: 'Sahar' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /create game/i })).toHaveAttribute('href', '/game');
    expect(screen.getByRole('link', { name: /profile/i })).toHaveAttribute('href', '/profile');
    expect(screen.getByRole('link', { name: /friends/i })).toHaveAttribute('href', '/friends');
    expect(screen.getByRole('button', { name: /log out/i })).toBeInTheDocument();
    expect(container.firstElementChild).toHaveClass('theme-classic-green-felt');
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

  it('loads the profile route directly and saves edited profile fields', async () => {
    window.history.pushState({}, '', '/profile');
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/me/profile') && init?.method === 'PATCH') {
        return Response.json({
          ...currentUser,
          name: 'Card Player',
          preferredTheme: 'DARK_CARD_ROOM'
        });
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByRole('heading', { name: /edit profile/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toHaveValue('sahar@example.com');

    fireEvent.change(screen.getByLabelText(/name/i), { target: { value: 'Card Player' } });
    fireEvent.click(screen.getByLabelText(/dark card room/i));
    fireEvent.click(screen.getByRole('button', { name: /save profile/i }));

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith('http://localhost:8080/api/me/profile', {
        method: 'PATCH',
        body: JSON.stringify({
          name: 'Card Player',
          preferredTheme: 'DARK_CARD_ROOM'
        }),
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });
    });
    expect(await screen.findByRole('heading', { name: 'Card Player' })).toBeInTheDocument();
  });

  it('loads the friends route directly and shows friend lists', async () => {
    window.history.pushState({}, '', '/friends');
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/friends')) {
        return Response.json({
          friends: [friend],
          incomingRequests: [incomingRequest],
          outgoingRequests: []
        });
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByRole('heading', { level: 1, name: /friends/i })).toBeInTheDocument();
    expect(screen.getAllByText('friend@example.com')).toHaveLength(2);
    expect(screen.getByText('Want to play Pasoor?')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /send friend request/i })).toBeInTheDocument();
  });

  it('sends a friend request and shows it as outgoing', async () => {
    window.history.pushState({}, '', '/friends');
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/friends') && !init?.method) {
        return Response.json(emptyFriends);
      }
      if (url.endsWith('/api/friends/requests') && init?.method === 'POST') {
        return Response.json({
          friends: [],
          incomingRequests: [],
          outgoingRequests: [
            {
              id: '277fbb47-e518-40d9-859d-cdbd5efc532b',
              requester: currentUser,
              recipient: friend,
              status: 'PENDING',
              message: 'Want to play?'
            }
          ]
        });
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByRole('heading', { level: 1, name: /friends/i })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'friend@example.com' } });
    fireEvent.change(screen.getByLabelText(/message/i), { target: { value: 'Want to play?' } });
    fireEvent.click(screen.getByRole('button', { name: /send friend request/i }));

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith('http://localhost:8080/api/friends/requests', {
        method: 'POST',
        body: JSON.stringify({
          email: 'friend@example.com',
          message: 'Want to play?'
        }),
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });
    });
    expect(await screen.findByText('friend@example.com')).toBeInTheDocument();
  });

  it('shows a friendly error when a friend request targets an unregistered email', async () => {
    window.history.pushState({}, '', '/friends');
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/friends') && !init?.method) {
        return Response.json(emptyFriends);
      }
      if (url.endsWith('/api/friends/requests') && init?.method === 'POST') {
        return Response.json(
          {
            timestamp: '2026-05-04T04:44:05.625+00:00',
            status: 404,
            error: 'Not Found',
            path: '/api/friends/requests'
          },
          { status: 404 }
        );
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByRole('heading', { level: 1, name: /friends/i })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/email/i), { target: { value: 'newfriend@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: /send friend request/i }));

    expect(await screen.findByText('That email is not registered yet. Ask them to sign in to Pasoor first.')).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toHaveValue('newfriend@example.com');
    expect(screen.queryByText('{"timestamp"')).not.toBeInTheDocument();
  });

  it('accepts an incoming friend request', async () => {
    window.history.pushState({}, '', '/friends');
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/friends') && !init?.method) {
        return Response.json({
          friends: [],
          incomingRequests: [incomingRequest],
          outgoingRequests: []
        });
      }
      if (url.endsWith(`/api/friends/requests/${incomingRequest.id}/accept`) && init?.method === 'POST') {
        return Response.json({
          friends: [friend],
          incomingRequests: [],
          outgoingRequests: []
        });
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    fireEvent.click(await screen.findByRole('button', { name: /accept/i }));

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith(`http://localhost:8080/api/friends/requests/${incomingRequest.id}/accept`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });
    });
    expect(await screen.findByText('friend@example.com')).toBeInTheDocument();
    expect(screen.queryByText('Want to play Pasoor?')).not.toBeInTheDocument();
  });

  it('applies the selected theme class from the authenticated user', async () => {
    window.history.pushState({}, '', '/dashboard');
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      Response.json({
        ...currentUser,
        preferredTheme: 'PERSIAN_TILE'
      })
    );

    const { container } = render(<App />);

    expect(await screen.findByRole('heading', { name: 'Sahar' })).toBeInTheDocument();
    expect(container.firstElementChild).toHaveClass('theme-persian-tile');
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
