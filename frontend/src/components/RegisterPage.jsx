import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../services/authService';

function Spinner() {
  return (
    <svg className="animate-spin" width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true" style={{ display: 'inline-block' }}>
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" strokeOpacity="0.25" />
      <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}

export default function RegisterPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    if (submitting) return;
    setError('');
    if (password !== confirm) {
      setError('Passwords do not match');
      return;
    }
    setSubmitting(true);
    try {
      const res = await authService.register(username, password);
      authService.saveToken(res.data.token);
      authService.saveRefreshToken(res.data.refreshToken);
      navigate('/dashboard');
    } catch (err) {
      const detail = err.response?.data?.detail;
      setError(detail || 'Registration failed');
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
            style={{ backgroundColor: 'rgba(236,72,153,0.08)', color: 'var(--color-accent)' }}
          >
            NEW ACCOUNT
          </span>
          <h1
            className="text-3xl font-bold tracking-tight"
            style={{ color: 'var(--color-primary)', fontFamily: "'Playfair Display', serif" }}
          >
            Create account
          </h1>
          <p className="text-sm" style={{ color: 'var(--color-text-subtle)' }}>
            Join AppSec Java Core
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="reg-username" className="block text-sm font-semibold mb-1.5" style={{ color: 'var(--color-text)' }}>
              Username
            </label>
            <input
              id="reg-username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              className="block w-full px-4 py-2.5 rounded-lg text-sm border-2 transition-all duration-200 focus:outline-none"
              style={{ backgroundColor: 'var(--color-bg)', color: 'var(--color-text)', borderColor: 'var(--color-border)' }}
            />
          </div>
          <div>
            <label htmlFor="reg-password" className="block text-sm font-semibold mb-1.5" style={{ color: 'var(--color-text)' }}>
              Password
            </label>
            <input
              id="reg-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
              className="block w-full px-4 py-2.5 rounded-lg text-sm border-2 transition-all duration-200 focus:outline-none"
              style={{ backgroundColor: 'var(--color-bg)', color: 'var(--color-text)', borderColor: 'var(--color-border)' }}
            />
          </div>
          <div>
            <label htmlFor="reg-confirm" className="block text-sm font-semibold mb-1.5" style={{ color: 'var(--color-text)' }}>
              Confirm password
            </label>
            <input
              id="reg-confirm"
              type="password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
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
            style={{ backgroundColor: 'var(--color-accent)' }}
          >
            <span className="inline-flex items-center justify-center gap-2">
              {submitting && <Spinner />}
              {submitting ? 'Creating account…' : 'Register'}
            </span>
          </button>
        </form>

        <p className="text-center text-sm" style={{ color: 'var(--color-text-subtle)' }}>
          Already have an account?{' '}
          <Link to="/login" className="font-semibold hover:underline" style={{ color: 'var(--color-secondary)' }}>
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
