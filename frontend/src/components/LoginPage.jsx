import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../services/authService';

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

function Spinner() {
  return (
    <svg className="animate-spin" width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true" style={{ display: 'inline-block' }}>
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" strokeOpacity="0.25" />
      <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}

function GoogleIcon() {
  return (
    <svg width="17" height="17" viewBox="0 0 18 18" aria-hidden="true" style={{ flexShrink: 0 }}>
      <path fill="#4285F4" d="M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.717v2.258h2.908C16.658 14.013 17.64 11.705 17.64 9.2Z"/>
      <path fill="#34A853" d="M9 18c2.43 0 4.467-.806 5.956-2.184l-2.908-2.258c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18Z"/>
      <path fill="#FBBC05" d="M3.964 10.707A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.707V4.961H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.039l3.007-2.332Z"/>
      <path fill="#EA4335" d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.961L3.964 7.293C4.672 5.163 6.656 3.58 9 3.58Z"/>
    </svg>
  );
}

function GitHubIcon() {
  return (
    <svg width="17" height="17" viewBox="0 0 24 24" aria-hidden="true" fill="currentColor" style={{ flexShrink: 0 }}>
      <path d="M12 0C5.374 0 0 5.373 0 12c0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23A11.509 11.509 0 0 1 12 5.803c1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576C20.566 21.797 24 17.3 24 12c0-6.627-5.373-12-12-12Z"/>
    </svg>
  );
}

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    if (submitting) return;
    setError('');
    setSubmitting(true);
    try {
      const res = await authService.login(username, password);
      authService.saveToken(res.data.token);
      authService.saveRefreshToken(res.data.refreshToken);
      navigate('/dashboard');
    } catch (err) {
      const status = err.response?.status;
      if (status === 423) {
        const secs = err.response?.data?.retryAfterSeconds ?? 900;
        setError(`Account locked. Try again in ${Math.ceil(secs / 60)} minutes.`);
      } else {
        setError('Invalid credentials');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div
      className="min-h-screen flex items-center justify-center px-4"
      style={{
        backgroundColor: 'var(--color-bg)',
        backgroundImage: 'radial-gradient(circle, var(--color-border) 1px, transparent 1px)',
        backgroundSize: '24px 24px',
      }}
    >
      <div
        className="w-full max-w-md rounded-2xl p-8 space-y-6"
        style={{
          backgroundColor: 'var(--color-surface)',
          border: '1px solid var(--color-border)',
          boxShadow: '0 24px 64px -12px rgba(0,0,0,0.1), 0 4px 16px -4px rgba(0,0,0,0.06)',
        }}
      >
        {/* Header */}
        <div className="text-center space-y-2">
          <span
            className="inline-block font-mono text-xs px-2.5 py-1 rounded-full font-bold tracking-widest uppercase mb-1"
            style={{ backgroundColor: 'rgba(99,102,241,0.08)', color: 'var(--color-secondary)' }}
          >
            SECURE
          </span>
          <h1
            className="text-3xl font-bold tracking-tight"
            style={{ color: 'var(--color-primary)', fontFamily: "'Playfair Display', serif" }}
          >
            AppSec Java Core
          </h1>
          <p className="text-sm" style={{ color: 'var(--color-text-subtle)' }}>
            Sign in to your account
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="username" className="block text-sm font-semibold mb-1.5" style={{ color: 'var(--color-text)' }}>
              Username
            </label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              className="block w-full px-4 py-2.5 rounded-lg text-sm border-2 transition-all duration-200 focus:outline-none"
              style={{ backgroundColor: 'var(--color-bg)', color: 'var(--color-text)', borderColor: 'var(--color-border)' }}
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-sm font-semibold mb-1.5" style={{ color: 'var(--color-text)' }}>
              Password
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="block w-full px-4 py-2.5 rounded-lg text-sm border-2 transition-all duration-200 focus:outline-none"
              style={{ backgroundColor: 'var(--color-bg)', color: 'var(--color-text)', borderColor: 'var(--color-border)' }}
            />
          </div>

          {error && (
            <p className="text-sm font-medium px-3 py-2.5 rounded-lg" style={{ backgroundColor: '#fee2e2', color: '#991b1b' }}>
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={submitting}
            className="w-full py-2.5 rounded-lg text-sm font-semibold text-white transition-all duration-200 hover:shadow-lg hover:scale-[1.01] active:scale-[0.99] disabled:opacity-70 disabled:cursor-not-allowed disabled:scale-100 cursor-pointer"
            style={{ backgroundColor: 'var(--color-secondary)' }}
          >
            <span className="inline-flex items-center justify-center gap-2">
              {submitting && <Spinner />}
              {submitting ? 'Signing in…' : 'Sign In'}
            </span>
          </button>
        </form>

        {/* Divider */}
        <div className="relative flex items-center gap-3">
          <div className="flex-1" style={{ borderTop: '1px solid var(--color-border)' }} />
          <span className="text-xs font-medium" style={{ color: 'var(--color-text-subtle)' }}>or continue with</span>
          <div className="flex-1" style={{ borderTop: '1px solid var(--color-border)' }} />
        </div>

        {/* OAuth */}
        <div className="space-y-2.5">
          <a
            href={`${API_URL}/oauth2/authorization/google`}
            className="flex items-center justify-center gap-3 w-full px-4 py-2.5 rounded-lg text-sm font-medium border-2 transition-all duration-200 hover:shadow-md hover:scale-[1.01] active:scale-[0.99]"
            style={{ borderColor: 'var(--color-border)', color: 'var(--color-text)', backgroundColor: 'var(--color-surface)' }}
          >
            <GoogleIcon />
            Sign in with Google
          </a>
          <a
            href={`${API_URL}/oauth2/authorization/github`}
            className="flex items-center justify-center gap-3 w-full px-4 py-2.5 rounded-lg text-sm font-medium border-2 transition-all duration-200 hover:shadow-md hover:scale-[1.01] active:scale-[0.99]"
            style={{ borderColor: 'var(--color-border)', color: 'var(--color-text)', backgroundColor: 'var(--color-surface)' }}
          >
            <GitHubIcon />
            Sign in with GitHub
          </a>
        </div>

        <p className="text-center text-sm" style={{ color: 'var(--color-text-subtle)' }}>
          No account?{' '}
          <Link to="/register" className="font-semibold hover:underline" style={{ color: 'var(--color-secondary)' }}>
            Register
          </Link>
        </p>
      </div>
    </div>
  );
}
