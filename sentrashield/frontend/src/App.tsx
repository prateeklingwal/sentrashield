import { useEffect, useState, type FormEvent } from 'react';
import {
  ApiError,
  authenticate,
  loadSession,
  saveSession,
  setUnauthorizedHandler,
  type Session,
} from './api';
import Dashboard from './Dashboard';
import { field, primary } from './ui';

function Login({ onSession }: { onSession: (s: Session) => void }) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const s = await authenticate(mode, email, password, name);
      saveSession(s);
      onSession(s);
    } catch (err) {
      const status = err instanceof ApiError ? err.status : 0;
      setError(
        status === 401
          ? 'Email or password is incorrect.'
          : status === 409
            ? 'That email is already registered. Log in instead.'
            : status === 400
              ? 'Enter an email and a password.'
              : 'Could not reach the server. Check that the backend is running.',
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto mt-24 w-full max-w-sm px-4">
      <h1 className="text-2xl font-semibold">SentraShield</h1>
      <p className="mb-6 mt-1 text-sm text-muted">See which npm dependencies put your project at risk.</p>
      <form onSubmit={submit} className="space-y-3">
        {mode === 'register' && (
          <input className={field} placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} />
        )}
        <input
          className={field}
          type="email"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <input
          className={field}
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        {error && <p className="text-sm text-bad">{error}</p>}
        <button className={`${primary} w-full`} disabled={busy}>
          {busy ? 'Please wait…' : mode === 'login' ? 'Log in' : 'Create account'}
        </button>
      </form>
      <button
        className="mt-4 text-sm text-muted underline"
        onClick={() => {
          setMode(mode === 'login' ? 'register' : 'login');
          setError('');
        }}
      >
        {mode === 'login' ? 'Create an account' : 'I already have an account'}
      </button>
    </div>
  );
}

export default function App() {
  const [session, setSession] = useState<Session | null>(loadSession);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      saveSession(null);
      setSession(null);
    });
  }, []);

  if (!session) return <Login onSession={setSession} />;

  return (
    <div className="mx-auto max-w-6xl px-4 py-6">
      <header className="mb-6 flex items-center justify-between">
        <h1 className="text-lg font-semibold">SentraShield</h1>
        <div className="flex items-center gap-3 text-sm text-muted">
          <span>{session.email}</span>
          <button
            className="underline"
            onClick={() => {
              saveSession(null);
              setSession(null);
            }}
          >
            Log out
          </button>
        </div>
      </header>
      <Dashboard />
    </div>
  );
}
