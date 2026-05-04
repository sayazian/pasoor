import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { BrowserRouter, Link, Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { getCurrentUser, googleLoginUrl, logout } from './api/authApi';
import GamePage from './pages/GamePage';
import type { CurrentUser } from './types/auth';

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

  return (
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
          <Link to="/dashboard">Profile</Link>
          <Link to="/dashboard">Friends</Link>
          <button type="button" onClick={handleLogout}>
            Log out
          </button>
        </nav>
      </section>
    </main>
  );
}
