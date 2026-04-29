import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../services/apiClient';

function Spinner({ size = 14 }) {
  return (
    <svg className="animate-spin" width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true" style={{ display: 'inline-block' }}>
      <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" strokeOpacity="0.25" />
      <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
    </svg>
  );
}

function RoleBadge({ role }) {
  const isAdmin = role === 'ADMIN';
  return (
    <span
      className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold tracking-widest uppercase font-mono"
      style={{
        backgroundColor: isAdmin ? 'rgba(99,102,241,0.1)' : 'rgba(107,114,128,0.1)',
        color: isAdmin ? 'var(--color-secondary)' : 'var(--color-text-subtle)',
      }}
    >
      {role}
    </span>
  );
}

function formatDate(isoString) {
  return new Date(isoString).toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' });
}

function ProfileSkeleton() {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <div className="w-16 h-16 rounded-2xl animate-pulse" style={{ backgroundColor: 'var(--color-border)' }} />
        <div className="space-y-2">
          <div className="h-6 w-32 rounded animate-pulse" style={{ backgroundColor: 'var(--color-border)' }} />
          <div className="h-4 w-20 rounded animate-pulse" style={{ backgroundColor: 'var(--color-border)' }} />
        </div>
      </div>
      {[1, 2, 3].map(i => (
        <div key={i} className="h-14 rounded-lg animate-pulse" style={{ backgroundColor: 'var(--color-border)' }} />
      ))}
    </div>
  );
}

export default function ProfilePage() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [pwForm, setPwForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' });
  const [pwError, setPwError] = useState('');
  const [pwSuccess, setPwSuccess] = useState(false);
  const [pwSubmitting, setPwSubmitting] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    apiClient.get('/api/v1/users/me')
      .then(res => { setProfile(res.data); setLoading(false); })
      .catch(() => { setError('Failed to load profile'); setLoading(false); });
  }, []);

  async function handlePasswordChange(e) {
    e.preventDefault();
    if (pwSubmitting) return;
    setPwError('');
    setPwSuccess(false);
    if (pwForm.newPassword !== pwForm.confirmPassword) {
      setPwError('New passwords do not match');
      return;
    }
    setPwSubmitting(true);
    try {
      await apiClient.patch('/api/v1/users/me/password', {
        currentPassword: pwForm.currentPassword,
        newPassword: pwForm.newPassword,
        confirmPassword: pwForm.confirmPassword,
      });
      setPwSuccess(true);
      setPwForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      const detail = err.response?.data?.detail || err.response?.data?.errors?.[0] || 'Failed to change password';
      setPwError(detail);
    } finally {
      setPwSubmitting(false);
    }
  }

  const initials = profile?.username?.slice(0, 2).toUpperCase() ?? '??';

  return (
    <div className="min-h-screen" style={{ backgroundColor: 'var(--color-bg)' }}>
      <div className="max-w-2xl mx-auto p-4 sm:p-8">

        {/* Back nav */}
        <button
          onClick={() => navigate('/dashboard')}
          className="flex items-center gap-2 text-sm font-medium mb-8 transition-opacity duration-200 hover:opacity-70"
          style={{ color: 'var(--color-text-subtle)' }}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M19 12H5M12 5l-7 7 7 7" />
          </svg>
          Back to Dashboard
        </button>

        <h1 className="text-4xl font-bold mb-8" style={{ color: 'var(--color-primary)', fontFamily: "'Playfair Display', serif" }}>
          My Profile
        </h1>

        {loading && (
          <div className="rounded-2xl p-6 border" style={{ backgroundColor: 'var(--color-surface)', borderColor: 'var(--color-border)' }}>
            <ProfileSkeleton />
          </div>
        )}

        {error && (
          <div className="flex flex-col items-center justify-center py-12 space-y-3 rounded-xl border" style={{ borderColor: 'rgba(239,68,68,0.2)', backgroundColor: 'rgba(239,68,68,0.04)' }}>
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#dc2626" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <p className="text-sm font-semibold" style={{ color: '#dc2626' }}>{error}</p>
            <button onClick={() => window.location.reload()} className="text-xs font-semibold px-3 py-1.5 rounded-lg" style={{ color: 'white', backgroundColor: '#dc2626' }}>Retry</button>
          </div>
        )}

        {profile && (
          <div className="space-y-6">
            {/* Identity card */}
            <div className="rounded-2xl p-6 border" style={{ backgroundColor: 'var(--color-surface)', borderColor: 'var(--color-border)', boxShadow: '0 4px 24px -8px rgba(0,0,0,0.06)' }}>
              <div className="flex items-center gap-5 mb-6">
                <div
                  className="w-16 h-16 rounded-2xl flex items-center justify-center text-xl font-bold text-white flex-shrink-0"
                  style={{ background: 'linear-gradient(135deg, var(--color-secondary), var(--color-accent))' }}
                >
                  {initials}
                </div>
                <div>
                  <div className="flex items-center gap-2.5 flex-wrap">
                    <h2 className="text-2xl font-bold" style={{ color: 'var(--color-primary)', fontFamily: "'Playfair Display', serif" }}>
                      {profile.username}
                    </h2>
                    <RoleBadge role={profile.role} />
                  </div>
                  <p className="text-sm mt-0.5" style={{ color: 'var(--color-text-subtle)' }}>
                    Member since {formatDate(profile.createdAt)}
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="rounded-xl p-4" style={{ backgroundColor: 'var(--color-bg)', border: '1px solid var(--color-border)' }}>
                  <p className="text-xs font-semibold uppercase tracking-wide mb-1.5" style={{ color: 'var(--color-text-subtle)' }}>User ID</p>
                  <p className="font-mono text-sm font-semibold" style={{ color: 'var(--color-text)' }}>#{profile.id}</p>
                </div>
                <div className="rounded-xl p-4" style={{ backgroundColor: 'var(--color-bg)', border: '1px solid var(--color-border)' }}>
                  <p className="text-xs font-semibold uppercase tracking-wide mb-1.5" style={{ color: 'var(--color-text-subtle)' }}>Auth method</p>
                  <p className="text-sm font-semibold capitalize" style={{ color: 'var(--color-text)' }}>
                    {profile.provider ? (
                      <span className="inline-flex items-center gap-1.5">
                        <span className="w-2 h-2 rounded-full" style={{ backgroundColor: '#059669' }} />
                        {profile.provider}
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1.5">
                        <span className="w-2 h-2 rounded-full" style={{ backgroundColor: 'var(--color-secondary)' }} />
                        Local account
                      </span>
                    )}
                  </p>
                </div>
                {profile.email && (
                  <div className="rounded-xl p-4 sm:col-span-2" style={{ backgroundColor: 'var(--color-bg)', border: '1px solid var(--color-border)' }}>
                    <p className="text-xs font-semibold uppercase tracking-wide mb-1.5" style={{ color: 'var(--color-text-subtle)' }}>Email</p>
                    <p className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>{profile.email}</p>
                  </div>
                )}
              </div>
            </div>

            {/* Change password */}
            {!profile.provider && (
              <div className="rounded-2xl p-6 border" style={{ backgroundColor: 'var(--color-surface)', borderColor: 'var(--color-border)', boxShadow: '0 4px 24px -8px rgba(0,0,0,0.06)' }}>
                <h3 className="text-lg font-bold mb-1" style={{ color: 'var(--color-primary)', fontFamily: "'Playfair Display', serif" }}>Change Password</h3>
                <p className="text-sm mb-5" style={{ color: 'var(--color-text-subtle)' }}>Choose a strong password of at least 8 characters.</p>

                {pwSuccess && (
                  <div className="mb-4 px-4 py-3 rounded-lg text-sm font-medium" style={{ backgroundColor: 'rgba(16,185,129,0.08)', color: '#059669', border: '1px solid rgba(16,185,129,0.2)' }}>
                    Password updated successfully.
                  </div>
                )}
                {pwError && (
                  <div className="mb-4 px-4 py-3 rounded-lg text-sm font-medium" style={{ backgroundColor: '#fee2e2', color: '#991b1b' }}>
                    {pwError}
                  </div>
                )}

                <form onSubmit={handlePasswordChange} className="space-y-4">
                  {[
                    { id: 'cur', label: 'Current password', key: 'currentPassword' },
                    { id: 'new', label: 'New password', key: 'newPassword' },
                    { id: 'conf', label: 'Confirm new password', key: 'confirmPassword' },
                  ].map(({ id, label, key }) => (
                    <div key={id}>
                      <label htmlFor={`pw-${id}`} className="block text-sm font-semibold mb-1.5" style={{ color: 'var(--color-text)' }}>{label}</label>
                      <input
                        id={`pw-${id}`}
                        type="password"
                        value={pwForm[key]}
                        onChange={e => setPwForm(f => ({ ...f, [key]: e.target.value }))}
                        required
                        minLength={key !== 'currentPassword' ? 8 : undefined}
                        className="block w-full px-4 py-2.5 rounded-lg text-sm border-2 transition-all duration-200 focus:outline-none"
                        style={{ backgroundColor: 'var(--color-bg)', color: 'var(--color-text)', borderColor: 'var(--color-border)' }}
                      />
                    </div>
                  ))}
                  <button
                    type="submit"
                    disabled={pwSubmitting}
                    className="px-6 py-2.5 rounded-lg text-sm font-semibold text-white transition-all duration-200 hover:shadow-lg hover:scale-[1.01] active:scale-[0.99] disabled:opacity-70 disabled:cursor-not-allowed cursor-pointer"
                    style={{ backgroundColor: 'var(--color-secondary)' }}
                  >
                    <span className="inline-flex items-center gap-2">
                      {pwSubmitting && <Spinner />}
                      {pwSubmitting ? 'Saving…' : 'Update password'}
                    </span>
                  </button>
                </form>
              </div>
            )}

            {profile.provider && (
              <div className="rounded-2xl p-5 border" style={{ borderColor: 'var(--color-border)', backgroundColor: 'var(--color-surface)' }}>
                <p className="text-sm" style={{ color: 'var(--color-text-subtle)' }}>
                  Password management is not available for OAuth2 accounts. Your identity is managed by <strong className="capitalize" style={{ color: 'var(--color-text)' }}>{profile.provider}</strong>.
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
