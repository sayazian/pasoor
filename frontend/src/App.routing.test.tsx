import '@testing-library/jest-dom/vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import type { GameState } from './types/game';
import type { MatchState } from './types/match';

const currentUser = {
  id: 'e51b2b5a-5797-40be-8771-caa189a2f8bd',
  name: 'Sahar',
  email: 'sahar@example.com',
  avatarUrl: null,
  preferredTheme: 'CLASSIC_GREEN_FELT'
};

const newGameState: GameState = {
  deck: [],
  deckCount: 40,
  myHand: [
    { id: 'CLUBS-TWO', suit: 'CLUBS', rank: 'TWO', value: 2 },
    { id: 'CLUBS-THREE', suit: 'CLUBS', rank: 'THREE', value: 3 },
    { id: 'CLUBS-FOUR', suit: 'CLUBS', rank: 'FOUR', value: 4 },
    { id: 'CLUBS-FIVE', suit: 'CLUBS', rank: 'FIVE', value: 5 }
  ],
  opponentHand: [],
  opponentHandCount: 4,
  tableCards: [
    { id: 'HEARTS-TWO', suit: 'HEARTS', rank: 'TWO', value: 2 },
    { id: 'HEARTS-THREE', suit: 'HEARTS', rank: 'THREE', value: 3 },
    { id: 'HEARTS-FOUR', suit: 'HEARTS', rank: 'FOUR', value: 4 },
    { id: 'HEARTS-FIVE', suit: 'HEARTS', rank: 'FIVE', value: 5 }
  ],
  myCollectedPile: [],
  opponentCollectedPile: [],
  currentTurn: 'ME',
  phase: 'PLAYING',
  initialDealDone: true,
  mySurCount: 0,
  opponentSurCount: 0,
  pendingCapturePlayer: null,
  pendingCaptureCard: null,
  lastCapturePlayer: null,
  score: null
};

const newMatchState: MatchState = {
  id: '7ac598a2-37cc-4af7-bf2f-a6ea00ab97d4',
  status: 'ACTIVE',
  playerOne: currentUser,
  playerTwo: null,
  viewerSide: 'PLAYER_ONE',
  playerOneTotalScore: 0,
  playerTwoTotalScore: 0,
  winnerSide: null,
  winner: null,
  exitedBy: null,
  playerOneEndChoice: null,
  playerTwoEndChoice: null,
  rematchId: null,
  currentRound: {
    id: '6b37b4ea-64dc-4965-b497-30db3b55f132',
    roundNumber: 1,
    status: 'ACTIVE',
    gameState: newGameState,
    playerOneRoundScore: null,
    playerTwoRoundScore: null,
    playerOneAcknowledged: false,
    playerTwoAcknowledged: false
  },
  lastCompletedRound: null,
  completedRounds: []
};

const undealtMatchState: MatchState = {
  ...newMatchState,
  currentRound: {
    ...newMatchState.currentRound,
    gameState: {
      ...newGameState,
      deckCount: 52,
      myHand: [],
      opponentHand: [],
      opponentHandCount: 0,
      tableCards: [],
      phase: 'NEW',
      initialDealDone: false
    }
  }
};

const dealtMatchState: MatchState = {
  ...newMatchState,
  currentRound: {
    ...newMatchState.currentRound,
    gameState: {
      ...newGameState,
      deck: [],
      deckCount: 40,
      myHand: [
        { id: 'CLUBS-TWO', suit: 'CLUBS', rank: 'TWO', value: 2 },
        { id: 'CLUBS-THREE', suit: 'CLUBS', rank: 'THREE', value: 3 },
        { id: 'CLUBS-FOUR', suit: 'CLUBS', rank: 'FOUR', value: 4 },
        { id: 'CLUBS-FIVE', suit: 'CLUBS', rank: 'FIVE', value: 5 }
      ],
      opponentHand: [],
      opponentHandCount: 4,
      tableCards: [
        { id: 'HEARTS-TWO', suit: 'HEARTS', rank: 'TWO', value: 2 },
        { id: 'HEARTS-THREE', suit: 'HEARTS', rank: 'THREE', value: 3 },
        { id: 'HEARTS-FOUR', suit: 'HEARTS', rank: 'FOUR', value: 4 },
        { id: 'HEARTS-FIVE', suit: 'HEARTS', rank: 'FIVE', value: 5 }
      ],
      phase: 'PLAYING',
      initialDealDone: true
    }
  }
};

const betweenHandsMatchState: MatchState = {
  ...newMatchState,
  currentRound: {
    ...newMatchState.currentRound,
    gameState: {
      ...newGameState,
      deckCount: 32,
      myHand: [],
      opponentHand: [],
      opponentHandCount: 0,
      tableCards: [
        { id: 'HEARTS-TWO', suit: 'HEARTS', rank: 'TWO', value: 2 },
        { id: 'SPADES-KING', suit: 'SPADES', rank: 'KING', value: 13 }
      ],
      phase: 'PLAYING',
      initialDealDone: true
    }
  }
};

const nextHandMatchState: MatchState = {
  ...newMatchState,
  currentRound: {
    ...newMatchState.currentRound,
    gameState: {
      ...newGameState,
      deckCount: 24,
      phase: 'PLAYING',
      initialDealDone: true
    }
  }
};

const waitingMatchState: MatchState = {
  ...newMatchState,
  status: 'WAITING'
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
  avatarUrl: null,
  online: true
};

const incomingRequest = {
  id: '277fbb47-e518-40d9-859d-cdbd5efc532b',
  requester: friend,
  recipient: currentUser,
  status: 'PENDING',
  message: 'Want to play Pasoor?'
};

const liveInvite = {
  id: '58f4dfeb-e064-454e-aa9b-7517ad8e120f',
  status: 'INVITED',
  sender: currentUser,
  recipient: friend,
  recipientEmail: 'friend@example.com',
  token: 'invite-token',
  match: waitingMatchState
};

const emptyGameInvites = {
  liveInvites: []
};

const incomingGameInvite = {
  ...liveInvite,
  id: '6119e214-e57d-49ba-9316-1e712815b56b',
  sender: friend,
  recipient: currentUser,
  recipientEmail: currentUser.email
};

describe('App routing', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    window.history.pushState({}, '', '/');
  });

  it('automatically deals when a match still arrives in the old undealt state', async () => {
    window.history.pushState({}, '', '/game');
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/matches') && init?.method === 'POST') {
        return Response.json(undealtMatchState);
      }
      if (
        url.endsWith(`/api/matches/${newMatchState.id}/rounds/${newMatchState.currentRound.id}/deal`) &&
        init?.method === 'POST'
      ) {
        return Response.json(dealtMatchState);
      }
      if (url.endsWith(`/api/matches/${newMatchState.id}`)) {
        return Response.json(dealtMatchState);
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByText('40')).toBeInTheDocument();
    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith(
        `http://localhost:8080/api/matches/${newMatchState.id}/rounds/${newMatchState.currentRound.id}/deal`,
        {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json'
          }
        }
      );
    });
    expect(screen.getByText('Play a card')).toBeInTheDocument();
  });

  it('automatically deals the next hand when both hands are empty and cards remain in the deck', async () => {
    window.history.pushState({}, '', `/game/${newMatchState.id}`);
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith(`/api/matches/${newMatchState.id}`) && !init?.method) {
        return Response.json(betweenHandsMatchState);
      }
      if (
        url.endsWith(`/api/matches/${newMatchState.id}/rounds/${newMatchState.currentRound.id}/deal`) &&
        init?.method === 'POST'
      ) {
        return Response.json(nextHandMatchState);
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByText('24')).toBeInTheDocument();
    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith(
        `http://localhost:8080/api/matches/${newMatchState.id}/rounds/${newMatchState.currentRound.id}/deal`,
        {
          method: 'POST',
          credentials: 'include',
          headers: {
            'Content-Type': 'application/json'
          }
        }
      );
    });
    expect(screen.getByText('Play a card')).toBeInTheDocument();
    expect(screen.getAllByText('4 cards')).toHaveLength(2);
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

  it('shows a live game invite popup after login and accepts it', async () => {
    window.history.pushState({}, '', '/dashboard');
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/invites') && !init?.method) {
        return Response.json({
          liveInvites: [incomingGameInvite]
        });
      }
      if (url.endsWith('/api/invites/invite-token/accept') && init?.method === 'POST') {
        return Response.json({
          ...incomingGameInvite,
          status: 'ACCEPTED',
          match: {
            ...waitingMatchState,
            status: 'ACTIVE',
            playerTwo: currentUser
          }
        });
      }
      if (url.endsWith(`/api/matches/${waitingMatchState.id}`) && !init?.method) {
        return Response.json({
          ...waitingMatchState,
          status: 'ACTIVE',
          playerTwo: currentUser
        });
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByRole('dialog')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /friend invited you/i })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^accept$/i }));

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith('http://localhost:8080/api/invites/invite-token/accept', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });
    });
    expect(await screen.findByText('Play a card')).toBeInTheDocument();
  });

  it('denies a live game invite from the popup', async () => {
    window.history.pushState({}, '', '/dashboard');
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/invites') && !init?.method) {
        return Response.json({
          liveInvites: [incomingGameInvite]
        });
      }
      if (url.endsWith('/api/invites/invite-token/decline') && init?.method === 'POST') {
        return Response.json({
          ...incomingGameInvite,
          status: 'DECLINED',
          match: {
            ...waitingMatchState,
            status: 'ABANDONED'
          }
        });
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    fireEvent.click(await screen.findByRole('button', { name: /deny/i }));

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith('http://localhost:8080/api/invites/invite-token/decline', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });
    });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('redirects protected routes to login for anonymous sessions', async () => {
    window.history.pushState({}, '', '/dashboard');
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 401 }));

    render(<App />);

    expect(await screen.findByRole('heading', { name: /sign in/i })).toBeInTheDocument();
  });

  it('loads the game route directly when the session is authenticated', async () => {
    window.history.pushState({}, '', '/game');
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/matches') && init?.method === 'POST') {
        return Response.json(newMatchState);
      }
      if (url.endsWith(`/api/matches/${newMatchState.id}`)) {
        return Response.json(newMatchState);
      }
      if (
        url.endsWith(`/api/matches/${newMatchState.id}/rounds/${newMatchState.currentRound.id}/deal`) &&
        init?.method === 'POST'
      ) {
        return Response.json(dealtMatchState);
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByText('Sahar turn')).toBeInTheDocument();
    expect(screen.getByLabelText(/match score/i)).toHaveTextContent('Round');
    expect(screen.getByLabelText(/match score/i)).toHaveTextContent('Sahar0');
    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith('http://localhost:8080/api/matches', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });
    });

    const deckButton = screen.getAllByRole('button', { name: /face-down card/i }).find((button) => button.hasAttribute('disabled'));
    expect(deckButton).toBeDefined();
    expect(await screen.findByText('40')).toBeInTheDocument();
  });

  it.each([
    ['CLASSIC_GREEN_FELT', 'theme-classic-green-felt'],
    ['MODERN_LIGHT_TABLE', 'theme-modern-light-table'],
    ['PERSIAN_TILE', 'theme-persian-tile'],
    ['DARK_CARD_ROOM', 'theme-dark-card-room']
  ] as const)('applies %s on the game route', async (preferredTheme, themeClass) => {
    window.history.pushState({}, '', '/game');
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json({
          ...currentUser,
          preferredTheme
        });
      }
      if (url.endsWith('/api/matches') && init?.method === 'POST') {
        return Response.json(newMatchState);
      }
      if (url.endsWith(`/api/matches/${newMatchState.id}`)) {
        return Response.json(newMatchState);
      }

      return new Response('', { status: 404 });
    });

    const { container } = render(<App />);

    expect(await screen.findByText('Play a card')).toBeInTheDocument();
    expect(container.firstElementChild).toHaveClass(themeClass);
  });

  it('loads an existing match route directly', async () => {
    window.history.pushState({}, '', `/game/${newMatchState.id}`);
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith(`/api/matches/${newMatchState.id}`)) {
        return Response.json(newMatchState);
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByText('Play a card')).toBeInTheDocument();
    expect(screen.getByText('Sahar turn')).toBeInTheDocument();
    expect(fetchSpy).not.toHaveBeenCalledWith(
      'http://localhost:8080/api/matches',
      expect.objectContaining({ method: 'POST' })
    );
  });

  it('returns to the dashboard after exiting a match', async () => {
    window.history.pushState({}, '', `/game/${newMatchState.id}`);
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/invites') && !init?.method) {
        return Response.json(emptyGameInvites);
      }
      if (url.endsWith(`/api/matches/${newMatchState.id}`) && !init?.method) {
        return Response.json(newMatchState);
      }
      if (url.endsWith(`/api/matches/${newMatchState.id}/exit`) && init?.method === 'POST') {
        return Response.json({
          ...newMatchState,
          status: 'ABANDONED'
        });
      }

      return new Response('', { status: 404 });
    });
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    render(<App />);

    fireEvent.click(await screen.findByRole('button', { name: /exit match/i }));

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith(`http://localhost:8080/api/matches/${newMatchState.id}/exit`, {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });
    });
    expect(await screen.findByRole('heading', { name: 'Sahar' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /create game/i })).toHaveAttribute('href', '/game');
  });

  it('shows a game load error instead of staying on loading text', async () => {
    window.history.pushState({}, '', '/game');
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/matches')) {
        return new Response('Backend unavailable', { status: 503 });
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByRole('heading', { name: /game could not be loaded/i })).toBeInTheDocument();
    expect(screen.getByText('Backend unavailable')).toBeInTheDocument();
    expect(screen.queryByText('Loading Pasoor...')).not.toBeInTheDocument();
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
      if (url.endsWith('/api/invites')) {
        return Response.json(emptyGameInvites);
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByRole('heading', { level: 1, name: /friends/i })).toBeInTheDocument();
    expect(screen.getAllByText('friend@example.com')).toHaveLength(2);
    expect(screen.getByText('Want to play Pasoor?')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /send friend request/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /invite to game/i })).toBeInTheDocument();
  });

  it('does not show invite to game for an offline friend', async () => {
    window.history.pushState({}, '', '/friends');
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/friends')) {
        return Response.json({
          friends: [{ ...friend, online: false }],
          incomingRequests: [],
          outgoingRequests: []
        });
      }
      if (url.endsWith('/api/invites')) {
        return Response.json(emptyGameInvites);
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByText('friend@example.com')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /invite to game/i })).not.toBeInTheDocument();
    expect(screen.getAllByText('Offline').length).toBeGreaterThan(0);
  });

  it('invites an accepted friend to a waiting match', async () => {
    window.history.pushState({}, '', '/friends');
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith('/api/friends') && !init?.method) {
        return Response.json({
          friends: [friend],
          incomingRequests: [],
          outgoingRequests: []
        });
      }
      if (url.endsWith('/api/invites') && !init?.method) {
        return Response.json(emptyGameInvites);
      }
      if (url.endsWith('/api/matches') && init?.method === 'POST') {
        return Response.json(newMatchState);
      }
      if (url.endsWith(`/api/matches/${newMatchState.id}/invite`) && init?.method === 'POST') {
        return Response.json(liveInvite);
      }
      if (url.endsWith(`/api/matches/${newMatchState.id}`) && !init?.method) {
        return Response.json(waitingMatchState);
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    fireEvent.click(await screen.findByRole('button', { name: /invite to game/i }));

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalledWith(`http://localhost:8080/api/matches/${newMatchState.id}/invite`, {
        method: 'POST',
        body: JSON.stringify({ email: 'friend@example.com' }),
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json'
        }
      });
    });
    expect(await screen.findByRole('heading', { name: /waiting for friend/i })).toBeInTheDocument();
    expect(screen.getByText(/your friend has been invited/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /resend invite/i })).not.toBeInTheDocument();
  });

  it('informs the inviter when a live game invite is denied', async () => {
    window.history.pushState(
      { usr: { inviteToken: liveInvite.token }, key: 'declined-invite' },
      '',
      `/game/${newMatchState.id}`
    );
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = input.toString();

      if (url.endsWith('/api/me')) {
        return Response.json(currentUser);
      }
      if (url.endsWith(`/api/matches/${newMatchState.id}`)) {
        return Response.json({
          ...waitingMatchState,
          status: 'ABANDONED'
        });
      }

      return new Response('', { status: 404 });
    });

    render(<App />);

    expect(await screen.findByText(/your friend denied the game invite/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /dashboard/i })).toHaveClass('secondary-action-button');
    expect(screen.getByRole('link', { name: /friends/i })).toHaveClass('secondary-action-button');
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
      if (url.endsWith('/api/invites') && !init?.method) {
        return Response.json(emptyGameInvites);
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
      if (url.endsWith('/api/invites') && !init?.method) {
        return Response.json(emptyGameInvites);
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
      if (url.endsWith('/api/invites') && !init?.method) {
        return Response.json(emptyGameInvites);
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
