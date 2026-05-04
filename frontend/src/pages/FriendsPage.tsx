import { useEffect, useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { acceptFriendRequest, getFriends, rejectFriendRequest, sendFriendRequest } from '../api/friendsApi';
import { ApiError } from '../api/http';
import { createMatch, inviteFriendToMatch } from '../api/matchesApi';
import type { FriendRequestSummary, FriendSummary, FriendsResponse } from '../types/friend';

const emptyFriends: FriendsResponse = {
  friends: [],
  incomingRequests: [],
  outgoingRequests: []
};

export default function FriendsPage() {
  const navigate = useNavigate();
  const [friends, setFriends] = useState<FriendsResponse>(emptyFriends);
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState('');
  const [status, setStatus] = useState<'loading' | 'idle' | 'saving'>('loading');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    getFriends()
      .then((response) => {
        if (!cancelled) {
          setFriends(response);
          setStatus('idle');
        }
      })
      .catch((caught) => {
        if (!cancelled) {
          setError(caught instanceof Error ? caught.message : 'Friends could not be loaded.');
          setStatus('idle');
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  async function run(action: () => Promise<FriendsResponse>, fallback: string) {
    setStatus('saving');
    setError(null);

    try {
      const response = await action();
      setFriends(response);
      setStatus('idle');
      return true;
    } catch (caught) {
      setError(getFriendlyError(caught, fallback));
      setStatus('idle');
      return false;
    }
  }

  async function handleSendRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const sent = await run(() => sendFriendRequest(email, message), 'Friend request could not be sent.');

    if (sent) {
      setEmail('');
      setMessage('');
    }
  }

  async function handleInviteFriend(friend: FriendSummary) {
    setStatus('saving');
    setError(null);

    try {
      const match = await createMatch();
      const invite = await inviteFriendToMatch(match.id, friend.email);
      navigate(`/game/${invite.match.id}`, { state: { inviteLink: invite.inviteLink } });
    } catch (caught) {
      setError(getFriendlyError(caught, 'Friend could not be invited.'));
      setStatus('idle');
    }
  }

  if (status === 'loading') {
    return <main className="loading">Loading friends...</main>;
  }

  return (
    <main className="app-shell friends-shell">
      <section className="friends-panel">
        <header className="friends-header">
          <div>
            <p className="eyebrow">Friends</p>
            <h1>Friends</h1>
          </div>
          <Link to="/dashboard">Dashboard</Link>
        </header>

        {error && <p className="error-message">{error}</p>}

        <form className="friend-request-form" onSubmit={handleSendRequest}>
          <label className="field">
            <span>Email</span>
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="friend@example.com"
              required
            />
          </label>
          <label className="field">
            <span>Message</span>
            <textarea value={message} onChange={(event) => setMessage(event.target.value)} maxLength={500} />
          </label>
          <button type="submit" disabled={status === 'saving'}>
            Send friend request
          </button>
        </form>

        <FriendList
          title="Friends"
          friends={friends.friends}
          emptyLabel="No friends yet"
          actions={(friend) => (
            <button type="button" onClick={() => handleInviteFriend(friend)} disabled={status === 'saving'}>
              Invite to game
            </button>
          )}
        />
        <RequestList
          title="Incoming requests"
          requests={friends.incomingRequests}
          direction="incoming"
          emptyLabel="No incoming requests"
          actions={(request) => (
            <>
              <button
                type="button"
                onClick={() => run(() => acceptFriendRequest(request.id), 'Friend request could not be accepted.')}
              >
                Accept
              </button>
              <button
                type="button"
                onClick={() => run(() => rejectFriendRequest(request.id), 'Friend request could not be rejected.')}
              >
                Reject
              </button>
            </>
          )}
        />
        <RequestList
          title="Outgoing requests"
          requests={friends.outgoingRequests}
          direction="outgoing"
          emptyLabel="No outgoing requests"
        />
      </section>
    </main>
  );
}

function getFriendlyError(caught: unknown, fallback: string): string {
  if (caught instanceof ApiError) {
    if (caught.status === 404) {
      return 'That email is not registered yet. Ask them to sign in to Pasoor first.';
    }

    if (caught.status === 409) {
      return 'A pending or accepted friendship already exists for that email.';
    }
  }

  return caught instanceof Error ? caught.message : fallback;
}

function FriendList({
  title,
  friends,
  emptyLabel,
  actions
}: {
  title: string;
  friends: FriendSummary[];
  emptyLabel: string;
  actions?: (friend: FriendSummary) => ReactNode;
}) {
  return (
    <section className="friends-section">
      <h2>{title}</h2>
      {friends.length === 0 ? (
        <p className="empty-label">{emptyLabel}</p>
      ) : (
        <ul className="friend-list">
          {friends.map((friend) => (
            <li key={friend.id}>
              <div>
                <strong>{friend.name}</strong>
                <span>{friend.email}</span>
              </div>
              {actions && <div className="request-actions">{actions(friend)}</div>}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function RequestList({
  title,
  requests,
  direction,
  emptyLabel,
  actions
}: {
  title: string;
  requests: FriendRequestSummary[];
  direction: 'incoming' | 'outgoing';
  emptyLabel: string;
  actions?: (request: FriendRequestSummary) => ReactNode;
}) {
  return (
    <section className="friends-section">
      <h2>{title}</h2>
      {requests.length === 0 ? (
        <p className="empty-label">{emptyLabel}</p>
      ) : (
        <ul className="friend-list">
          {requests.map((request) => {
            const user = direction === 'incoming' ? request.requester : request.recipient;

            return (
              <li key={request.id}>
                <div>
                  <strong>{user.name}</strong>
                  <span>{user.email}</span>
                  {request.message && <p>{request.message}</p>}
                </div>
                {actions && <div className="request-actions">{actions(request)}</div>}
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
