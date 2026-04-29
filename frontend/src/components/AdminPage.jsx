import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../services/apiClient';
import { authService } from '../services/authService';

const PAGE_SIZE = 20;

function RoleBadge({ role }) {
  const isAdmin = role === 'ADMIN';
  return (
    <span
      className="inline-flex items-center px-2 py-0.5 rounded font-mono text-xs font-bold tracking-wide"
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
  return new Date(isoString).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function TableSkeleton() {
  return (
    <div className="space-y-2">
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="h-14 rounded-lg animate-pulse" style={{ backgroundColor: 'var(--color-border)', opacity: 1 - i * 0.08 }} />
      ))}
    </div>
  );
}

const INITIAL_RESULT = { key: null, users: [], totalPages: 0, totalElements: 0, error: '' };

export default function AdminPage() {
  const [page, setPage] = useState(0);
  const [retryKey, setRetryKey] = useState(0);
  const [result, setResult] = useState(INITIAL_RESULT);
  const [updating, setUpdating] = useState(null);
  const navigate = useNavigate();
  const currentUsername = authService.getUsername();

  const fetchKey = `${page}|${retryKey}`;
  const isLoading = fetchKey !== result.key;
  const users       = isLoading ? [] : result.users;
  const totalPages  = isLoading ? 0  : result.totalPages;
  const totalElements = isLoading ? 0 : result.totalElements;
  const error       = isLoading ? '' : result.error;

  useEffect(() => {
    let cancelled = false;
    const key = `${page}|${retryKey}`;

    apiClient.get(`/api/v1/admin/users?page=${page}&size=${PAGE_SIZE}`)
      .then(res => {
        if (cancelled) return;
        setResult({ key, users: res.data.content, totalPages: res.data.totalPages, totalElements: res.data.totalElements, error: '' });
      })
      .catch(() => {
        if (cancelled) return;
        setResult({ key, users: [], totalPages: 0, totalElements: 0, error: 'Failed to load users' });
      });

    return () => { cancelled = true; };
  }, [page, retryKey]);

  async function toggleRole(user) {
    if (updating) return;
    const newRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
    setUpdating(user.id);
    try {
      const res = await apiClient.patch(`/api/v1/admin/users/${user.id}/role`, { role: newRole });
      setResult(prev => ({
        ...prev,
        users: prev.users.map(u => u.id === user.id ? res.data : u),
      }));
    } catch {
      // role unchanged
    } finally {
      setUpdating(null);
    }
  }

  const isSelf = (username) => username === currentUsername;

  return (
    <div className="min-h-screen" style={{ backgroundColor: 'var(--color-bg)' }}>
      <div className="max-w-5xl mx-auto p-4 sm:p-8">

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

        <div className="flex flex-col sm:flex-row sm:items-baseline sm:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-4xl font-bold mb-1" style={{ color: 'var(--color-primary)', fontFamily: "'Playfair Display', serif" }}>
              User Management
            </h1>
            {totalElements > 0 && (
              <p className="text-sm" style={{ color: 'var(--color-text-subtle)' }}>
                {totalElements} registered {totalElements === 1 ? 'user' : 'users'}
              </p>
            )}
          </div>
          <span
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold tracking-widest uppercase font-mono self-start"
            style={{ backgroundColor: 'rgba(239,68,68,0.08)', color: '#dc2626' }}
          >
            <span className="w-1.5 h-1.5 rounded-full bg-red-500 animate-pulse" />
            ADMIN ONLY
          </span>
        </div>

        {isLoading && (
          <div className="rounded-2xl p-6 border" style={{ backgroundColor: 'var(--color-surface)', borderColor: 'var(--color-border)' }}>
            <TableSkeleton />
          </div>
        )}

        {!isLoading && error && (
          <div className="flex flex-col items-center justify-center py-16 space-y-4 rounded-xl border" style={{ borderColor: 'rgba(239,68,68,0.2)', backgroundColor: 'rgba(239,68,68,0.04)' }}>
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#dc2626" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <p className="text-sm font-semibold" style={{ color: '#dc2626' }}>{error}</p>
            <button onClick={() => setRetryKey(k => k + 1)} className="text-xs font-semibold px-4 py-2 rounded-lg" style={{ color: 'white', backgroundColor: '#dc2626' }}>Retry</button>
          </div>
        )}

        {!isLoading && !error && (
          <>
            {/* Table — md+ */}
            <div className="hidden md:block rounded-xl overflow-hidden border" style={{ borderColor: 'var(--color-border)', backgroundColor: 'var(--color-surface)' }}>
              <table className="w-full text-sm text-left">
                <thead style={{ backgroundColor: 'var(--color-bg)', borderBottom: `2px solid var(--color-border)` }}>
                  <tr>
                    {['User', 'Role', 'Joined', 'Auth', 'Actions'].map(col => (
                      <th key={col} className="px-6 py-4 font-semibold tracking-wide uppercase text-xs" style={{ color: 'var(--color-text-subtle)' }}>{col}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y" style={{ borderColor: 'var(--color-border)' }}>
                  {users.length === 0 ? (
                    <tr><td colSpan={5} className="px-6 py-10 text-center text-sm" style={{ color: 'var(--color-text-subtle)' }}>No users found</td></tr>
                  ) : users.map((user, i) => (
                    <tr key={user.id} style={{ backgroundColor: i % 2 === 0 ? 'transparent' : 'var(--color-bg)' }}>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div
                            className="w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold text-white flex-shrink-0"
                            style={{ background: user.role === 'ADMIN' ? 'linear-gradient(135deg, var(--color-secondary), var(--color-accent))' : 'rgba(107,114,128,0.3)' }}
                          >
                            {user.username.slice(0, 2).toUpperCase()}
                          </div>
                          <div>
                            <p className="font-semibold" style={{ color: 'var(--color-text)' }}>{user.username}</p>
                            {isSelf(user.username) && <p className="text-xs" style={{ color: 'var(--color-secondary)' }}>You</p>}
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4"><RoleBadge role={user.role} /></td>
                      <td className="px-6 py-4 text-sm" style={{ color: 'var(--color-text-subtle)' }}>{formatDate(user.createdAt)}</td>
                      <td className="px-6 py-4 text-sm capitalize" style={{ color: 'var(--color-text-subtle)' }}>{user.provider ?? 'local'}</td>
                      <td className="px-6 py-4">
                        {isSelf(user.username) ? (
                          <span className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>—</span>
                        ) : (
                          <button
                            onClick={() => toggleRole(user)}
                            disabled={!!updating}
                            className="text-xs font-semibold px-3 py-1.5 rounded-lg transition-all duration-200 hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
                            style={{
                              backgroundColor: user.role === 'ADMIN' ? 'rgba(107,114,128,0.1)' : 'rgba(99,102,241,0.1)',
                              color: user.role === 'ADMIN' ? 'var(--color-text-subtle)' : 'var(--color-secondary)',
                            }}
                          >
                            {updating === user.id ? '…' : user.role === 'ADMIN' ? 'Make User' : 'Make Admin'}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Cards — mobile */}
            <div className="md:hidden space-y-3">
              {users.length === 0 ? (
                <div className="text-center py-8 text-sm" style={{ color: 'var(--color-text-subtle)' }}>No users found</div>
              ) : users.map(user => (
                <div key={user.id} className="rounded-xl p-4 border" style={{ backgroundColor: 'var(--color-surface)', borderColor: 'var(--color-border)' }}>
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-3">
                      <div
                        className="w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold text-white"
                        style={{ background: user.role === 'ADMIN' ? 'linear-gradient(135deg, var(--color-secondary), var(--color-accent))' : 'rgba(107,114,128,0.3)' }}
                      >
                        {user.username.slice(0, 2).toUpperCase()}
                      </div>
                      <div>
                        <p className="font-semibold text-sm" style={{ color: 'var(--color-text)' }}>{user.username}</p>
                        {isSelf(user.username) && <p className="text-xs" style={{ color: 'var(--color-secondary)' }}>You</p>}
                      </div>
                    </div>
                    <RoleBadge role={user.role} />
                  </div>
                  <div className="flex items-center justify-between">
                    <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>{formatDate(user.createdAt)} · {user.provider ?? 'local'}</p>
                    {!isSelf(user.username) && (
                      <button
                        onClick={() => toggleRole(user)}
                        disabled={!!updating}
                        className="text-xs font-semibold px-3 py-1.5 rounded-lg transition-all duration-200 hover:opacity-80 disabled:opacity-50"
                        style={{
                          backgroundColor: user.role === 'ADMIN' ? 'rgba(107,114,128,0.1)' : 'rgba(99,102,241,0.1)',
                          color: user.role === 'ADMIN' ? 'var(--color-text-subtle)' : 'var(--color-secondary)',
                        }}
                      >
                        {updating === user.id ? '…' : user.role === 'ADMIN' ? 'Make User' : 'Make Admin'}
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-between gap-4 mt-6">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="px-4 py-2 text-sm font-medium rounded-lg transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-md"
                  style={{ backgroundColor: 'var(--color-surface)', color: 'var(--color-primary)', border: `1px solid var(--color-border)` }}
                >
                  Previous
                </button>
                <span className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>
                  Page {page + 1} of {totalPages}
                </span>
                <button
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1}
                  className="px-4 py-2 text-sm font-medium rounded-lg transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-md"
                  style={{ backgroundColor: 'var(--color-surface)', color: 'var(--color-primary)', border: `1px solid var(--color-border)` }}
                >
                  Next
                </button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
