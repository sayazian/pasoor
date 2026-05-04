import { useEffect, useState } from 'react';
import type { FormEvent, ReactNode } from 'react';
import { BrowserRouter, Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { getCurrentUser, googleLoginUrl, logout, updateProfile } from './api/authApi';
import GamePage from './pages/GamePage';
import type { CurrentUser, PreferredTheme } from './types/auth';

type AuthStatus = 'loading' | 'anonymous' | 'authenticated';

export default function App() {
  const [authStatus, setAuthStatus] = useState<AuthStatus>('loading');
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);

  useEffect(() => {
    let cancelled = false;

    getCurrentUser()
      .then((user) => {
        if (cancelled) {
          return;
        }
        setCurrentUser(user);
        setAuthStatus(user ? 'authenticated' : 'anonymous');
      })
      .catch(() => {
        if (cancelled) {
          return;
        }
        setCurrentUser(null);
        setAuthStatus('anonymous');
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const themeClass = `theme-${themeSlug(currentUser?.preferredTheme ?? 'CLASSIC_GREEN_FELT')}`;

  return (
    <div className={`theme-root ${themeClass}`}>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage authStatus={authStatus} />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute authStatus={authStatus}>
                <DashboardPage
                  currentUser={currentUser!}
                  onLogout={() => {
                    setCurrentUser(null);
                    setAuthStatus('anonymous');
                  }}
                />
              </ProtectedRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <ProtectedRoute authStatus={authStatus}>
                <ProfilePage currentUser={currentUser!} onProfileUpdated={setCurrentUser} />
              </ProtectedRoute>
            }
          />
          <Route
            path="/game"
            element={
              <ProtectedRoute authStatus={authStatus}>
                <GamePage />
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<HomeRoute authStatus={authStatus} />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </div>
  );
}

function HomeRoute({ authStatus }: { authStatus: AuthStatus }) {
  if (authStatus === 'loading') {
    return <main className="loading">Loading Pasoor...</main>;
  }

  return <Navigate to={authStatus === 'authenticated' ? '/dashboard' : '/login'} replace />;
}

function ProtectedRoute({ authStatus, children }: { authStatus: AuthStatus; children: ReactNode }) {
  if (authStatus === 'loading') {
    return <main className="loading">Loading Pasoor...</main>;
  }
  if (authStatus === 'anonymous') {
    return <Navigate to="/login" replace />;
  }

  return children;
}

function LoginPage({ authStatus }: { authStatus: AuthStatus }) {
  if (authStatus === 'authenticated') {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <main className="auth-page">
      <section className="auth-panel">
        <p className="eyebrow">Pasoor</p>
        <h1>Sign in</h1>
        <a className="primary-link-button" href={googleLoginUrl()}>
          Continue with Google
        </a>
      </section>
    </main>
  );
}

function DashboardPage({ currentUser, onLogout }: { currentUser: CurrentUser; onLogout: () => void }) {
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    onLogout();
    navigate('/login', { replace: true });
  }

  return (
    <main className="app-shell dashboard-shell">
      <section className="dashboard-panel">
        <div>
          <p className="eyebrow">Dashboard</p>
          <h1>{currentUser.name}</h1>
          <p className="muted-text">{currentUser.email}</p>
        </div>

        <nav className="dashboard-actions" aria-label="Dashboard actions">
          <Link to="/game">Create Game</Link>
          <Link to="/profile">Profile</Link>
          <Link to="/dashboard">Friends</Link>
          <button type="button" onClick={handleLogout}>
            Log out
          </button>
        </nav>
      </section>
    </main>
  );
}

const themes: Array<{ value: PreferredTheme; label: string }> = [
  { value: 'CLASSIC_GREEN_FELT', label: 'Classic Green Felt' },
  { value: 'MODERN_LIGHT_TABLE', label: 'Modern Light Table' },
  { value: 'PERSIAN_TILE', label: 'Persian Tile' },
  { value: 'DARK_CARD_ROOM', label: 'Dark Card Room' }
];

function ProfilePage({
  currentUser,
  onProfileUpdated
}: {
  currentUser: CurrentUser;
  onProfileUpdated: (user: CurrentUser) => void;
}) {
  const navigate = useNavigate();
  const [name, setName] = useState(currentUser.name);
  const [preferredTheme, setPreferredTheme] = useState<PreferredTheme>(currentUser.preferredTheme);
  const [status, setStatus] = useState<'idle' | 'saving'>('idle');
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setStatus('saving');
    setError(null);

    try {
      const updatedUser = await updateProfile({ name, preferredTheme });
      onProfileUpdated(updatedUser);
      navigate('/dashboard');
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : 'Profile could not be saved.');
      setStatus('idle');
    }
  }

  return (
    <main className="app-shell profile-shell">
      <form className="profile-panel" onSubmit={handleSubmit}>
        <div>
          <p className="eyebrow">Profile</p>
          <h1>Edit profile</h1>
        </div>

        <label className="field">
          <span>Name</span>
          <input value={name} onChange={(event) => setName(event.target.value)} required />
        </label>

        <label className="field">
          <span>Email</span>
          <input value={currentUser.email} readOnly />
        </label>

        <fieldset className="theme-options">
          <legend>Game appearance</legend>
          {themes.map((theme) => (
            <label key={theme.value} className={`theme-option theme-preview-${themeSlug(theme.value)}`}>
              <input
                type="radio"
                name="preferredTheme"
                value={theme.value}
                checked={preferredTheme === theme.value}
                onChange={() => setPreferredTheme(theme.value)}
              />
              <span className="theme-swatch" aria-hidden="true" />
              <span>{theme.label}</span>
            </label>
          ))}
        </fieldset>

        {error && <p className="error-message">{error}</p>}

        <div className="profile-actions">
          <Link to="/dashboard">Cancel</Link>
          <button type="submit" disabled={status === 'saving'}>
            {status === 'saving' ? 'Saving...' : 'Save profile'}
          </button>
        </div>
      </form>
    </main>
  );
}

function themeSlug(theme: PreferredTheme) {
  return theme.toLowerCase().replaceAll('_', '-');
}
