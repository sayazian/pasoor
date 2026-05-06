import '@testing-library/jest-dom/vitest';
import { cleanup, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { MemoryRouter } from 'react-router-dom';
import FriendsPage from './FriendsPage';
import { getFriends } from '../api/friendsApi';
import type { FriendsResponse } from '../types/friend';

vi.mock('../api/friendsApi', () => ({
  getFriends: vi.fn(),
  sendFriendRequest: vi.fn(),
  acceptFriendRequest: vi.fn(),
  rejectFriendRequest: vi.fn()
}));

vi.mock('../api/matchesApi', () => ({
  createMatch: vi.fn(),
  inviteFriendToMatch: vi.fn()
}));

describe('FriendsPage', () => {
  afterEach(() => {
    cleanup();
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  it('refreshes the friend list while the page is open', async () => {
    vi.useFakeTimers();
    vi.mocked(getFriends)
      .mockResolvedValueOnce(emptyFriends())
      .mockResolvedValueOnce({
        friends: [friend('friend-1', 'New Friend', 'friend@example.com')],
        incomingRequests: [],
        outgoingRequests: []
      });

    render(
      <MemoryRouter>
        <FriendsPage />
      </MemoryRouter>
    );

    await vi.waitFor(() => expect(screen.getByText('No friends yet')).toBeInTheDocument());

    await vi.advanceTimersByTimeAsync(3000);

    await vi.waitFor(() => expect(screen.getByText('New Friend')).toBeInTheDocument());
    expect(getFriends).toHaveBeenCalledTimes(2);
  });
});

function emptyFriends(): FriendsResponse {
  return {
    friends: [],
    incomingRequests: [],
    outgoingRequests: []
  };
}

function friend(id: string, name: string, email: string) {
  return {
    id,
    name,
    email,
    avatarUrl: null,
    online: true
  };
}
