import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { acceptInvite, getInvite } from '../api/matchesApi';
import type { GameInvite } from '../types/invite';

export default function InvitePage() {
  const { token } = useParams();
  const navigate = useNavigate();
  const [invite, setInvite] = useState<GameInvite | null>(null);
  const [status, setStatus] = useState<'loading' | 'idle' | 'saving'>('loading');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    if (!token) {
      setError('Invite token is missing.');
      setStatus('idle');
      return;
    }

    getInvite(token)
      .then((response) => {
        if (!cancelled) {
          setInvite(response);
          setStatus('idle');
        }
      })
      .catch((caught) => {
        if (!cancelled) {
          setError(caught instanceof Error ? caught.message : 'Invite could not be loaded.');
          setStatus('idle');
        }
      });

    return () => {
      cancelled = true;
    };
  }, [token]);

  async function handleAcceptInvite() {
    if (!token) {
      return;
    }
    setStatus('saving');
    setError(null);

    try {
      const acceptedInvite = await acceptInvite(token);
      navigate(`/game/${acceptedInvite.match.id}`, { replace: true });
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Invite could not be accepted.');
      setStatus('idle');
    }
  }

  if (status === 'loading') {
    return <main className="loading">Loading invite...</main>;
  }

  return (
    <main className="app-shell invite-shell">
      <section className="invite-panel">
        <p className="eyebrow">Game invite</p>
        <h1>{invite ? `${invite.sender.name} invited you` : 'Invite unavailable'}</h1>
        {invite && <p className="muted-text">Join {invite.sender.name} for a Pasoor match.</p>}
        {error && <p className="error-message">{error}</p>}
        <div className="profile-actions">
          <Link to="/dashboard">Dashboard</Link>
          {invite?.status === 'PENDING' && (
            <button type="button" onClick={handleAcceptInvite} disabled={status === 'saving'}>
              {status === 'saving' ? 'Joining...' : 'Accept invite'}
            </button>
          )}
        </div>
      </section>
    </main>
  );
}
