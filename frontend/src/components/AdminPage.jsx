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

function StatusDot({ enabled }) {
  return (
    <span className="inline-flex items-center gap-1.5 text-xs font-medium"
      style={{ color: enabled ? '#059669' : '#dc2626' }}>
      <span className="w-1.5 h-1.5 rounded-full"
        style={{ backgroundColor: enabled ? '#059669' : '#dc2626' }} />
      {enabled ? 'Active' : 'Disabled'}
    </span>
  );
}

function formatDate(isoString) {
  if (!isoString) return '—';
  return new Date(isoString).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
}

function TableSkeleton() {
  return (
    <div className="space-y-2">
      {Array.from({ length: 8 }).map((_, i) => (
        <div key={i} className="h-14 rounded-lg animate-pulse"
          style={{ backgroundColor: 'var(--color-border)', opacity: 1 - i * 0.08 }} />
      ))}
    </div>
  );
}

function ConfirmModal({ username, onConfirm, onCancel }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center"
      style={{ backgroundColor: 'rgba(0,0,0,0.4)' }}>
      <div className="rounded-2xl p-6 w-full max-w-sm shadow-2xl"
        style={{ backgroundColor: 'var(--color-surface)', border: '1px solid var(--color-border)' }}>
        <h3 className="text-lg font-bold mb-2" style={{ color: 'var(--color-primary)', fontFamily: "'Playfair Display', serif" }}>
          Delete account?
        </h3>
        <p className="text-sm mb-6" style={{ color: 'var(--color-text-subtle)' }}>
          This will permanently delete <strong style={{ color: 'var(--color-text)' }}>{username}</strong>. This action cannot be undone.
        </p>
        <div className="flex gap-3 justify-end">
          <button onClick={onCancel}
            className="px-4 py-2 text-sm font-semibold rounded-lg"
            style={{ backgroundColor: 'var(--color-bg)', color: 'var(--color-text)', border: '1px solid var(--color-border)' }}>
            Cancel
          </button>
          <button onClick={onConfirm}
            className="px-4 py-2 text-sm font-semibold rounded-lg text-white"
            style={{ backgroundColor: '#dc2626' }}>
            Delete
          </button>
        </div>
      </div>
    </div>
  );
}

const INITIAL_RESULT = { key: null, users: [], totalPages: 0, totalElements: 0, error: '' };

export default function AdminPage() {
  const [page, setPage] = useState(0);
  const [retryKey, setRetryKey] = useState(0);
  const [result, setResult] = useState(INITIAL_RESULT);
  const [updating, setUpdating] = useState(null);
  const [confirmDelete, setConfirmDelete] = useState(null);
  const navigate = useNavigate();
  const currentUsername = authService.getUsername();

  const fetchKey = `${page}|${retryKey}`;
  const isLoading = fetchKey !== result.key;
  const users         = isLoading ? [] : result.users;
  const totalPages    = isLoading ? 0  : result.totalPages;
  const totalElements = isLoading ? 0  : result.totalElements;
  const error         = isLoading ? '' : result.error;

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
    setUpdating(`role-${user.id}`);
    try {
      const res = await apiClient.patch(`/api/v1/admin/users/${user.id}/role`, { role: newRole });
      setResult(prev => ({ ...prev, users: prev.users.map(u => u.id === user.id ? res.data : u) }));
    } catch { /* role unchanged */ } finally {
      setUpdating(null);
    }
  }

  async function toggleEnabled(user) {
    if (updating) return;
    setUpdating(`status-${user.id}`);
    try {
      const res = await apiClient.patch(`/api/v1/admin/users/${user.id}/status`, { enabled: !user.enabled });
      setResult(prev => ({ ...prev, users: prev.users.map(u => u.id === user.id ? res.data : u) }));
    } catch { /* unchanged */ } finally {
      setUpdating(null);
    }
  }

  async function deleteUser(user) {
    setConfirmDelete(null);
    setUpdating(`delete-${user.id}`);
    try {
      await apiClient.delete(`/api/v1/admin/users/${user.id}`);
      setResult(prev => ({ ...prev, users: prev.users.filter(u => u.id !== user.id), totalElements: prev.totalElements - 1 }));
    } catch { /* unchanged */ } finally {
      setUpdating(null);
    }
  }

  const isSelf = (username) => username === currentUsername;

  function ActionButtons({ user }) {
    if (isSelf(user.username)) return <span className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>—</span>;

    const busy = updating !== null;
    const roleLabel = updating === `role-${user.id}` ? '…' : user.role === 'ADMIN' ? 'Make User' : 'Make Admin';
    const statusLabel = updating === `status-${user.id}` ? '…' : user.enabled ? 'Disable' : 'Enable';

    return (
      <div className="flex items-center gap-1.5 flex-wrap">
        <button onClick={() => toggleRole(user)} disabled={busy}
          className="text-xs font-semibold px-2.5 py-1 rounded-lg transition-all duration-200 hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
          style={{ backgroundColor: user.role === 'ADMIN' ? 'rgba(107,114,128,0.1)' : 'rgba(99,102,241,0.1)', color: user.role === 'ADMIN' ? 'var(--color-text-subtle)' : 'var(--color-secondary)' }}>
          {roleLabel}
        </button>
        <button onClick={() => toggleEnabled(user)} disabled={busy}
          className="text-xs font-semibold px-2.5 py-1 rounded-lg transition-all duration-200 hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
          style={{ backgroundColor: user.enabled ? 'rgba(239,68,68,0.08)' : 'rgba(16,185,129,0.08)', color: user.enabled ? '#dc2626' : '#059669' }}>
          {statusLabel}
        </button>
        <button onClick={() => setConfirmDelete(user)} disabled={busy}
          className="text-xs font-semibold px-2.5 py-1 rounded-lg transition-all duration-200 hover:opacity-80 disabled:opacity-50 disabled:cursor-not-allowed"
          style={{ backgroundColor: 'rgba(239,68,68,0.06)', color: '#dc2626' }}
          aria-label={`Delete ${user.username}`}>
          {updating === `delete-${user.id}` ? '…' : 'Delete'}
        </button>
      </div>
    );
  }

  return (
    <div className="min-h-screen" style={{ backgroundColor: 'var(--color-bg)' }}>
      {confirmDelete && (
        <ConfirmModal
          username={confirmDelete.username}
          onConfirm={() => deleteUser(confirmDelete)}
          onCancel={() => setConfirmDelete(null)}
        />
      )}

      <div className="max-w-6xl mx-auto p-4 sm:p-8">

        <button onClick={() => navigate('/dashboard')}
          className="flex items-center gap-2 text-sm font-medium mb-8 transition-opacity duration-200 hover:opacity-70"
          style={{ color: 'var(--color-text-subtle)' }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M19 12H5M12 5l-7 7 7 7" />
          </svg>
          Back to Dashboard
        </button>

        <div className="flex flex-col sm:flex-row sm:items-baseline sm:justify-between gap-4 mb-8">
          <div>
            <h1 className="text-4xl font-bold mb-1"
              style={{ color: 'var(--color-primary)', fontFamily: "'Playfair Display', serif" }}>
              User Management
            </h1>
            {totalElements > 0 && (
              <p className="text-sm" style={{ color: 'var(--color-text-subtle)' }}>
                {totalElements} registered {totalElements === 1 ? 'user' : 'users'}
              </p>
            )}
          </div>
          <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-bold tracking-widest uppercase font-mono self-start"
            style={{ backgroundColor: 'rgba(239,68,68,0.08)', color: '#dc2626' }}>
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
          <div className="flex flex-col items-center justify-center py-16 space-y-4 rounded-xl border"
            style={{ borderColor: 'rgba(239,68,68,0.2)', backgroundColor: 'rgba(239,68,68,0.04)' }}>
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="#dc2626" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="12" cy="12" r="10" /><line x1="12" y1="8" x2="12" y2="12" /><line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <p className="text-sm font-semibold" style={{ color: '#dc2626' }}>{error}</p>
            <button onClick={() => setRetryKey(k => k + 1)} className="text-xs font-semibold px-4 py-2 rounded-lg"
              style={{ color: 'white', backgroundColor: '#dc2626' }}>Retry</button>
          </div>
        )}

        {!isLoading && !error && (
          <>
            {/* Table — md+ */}
            <div className="hidden md:block rounded-xl overflow-hidden border"
              style={{ borderColor: 'var(--color-border)', backgroundColor: 'var(--color-surface)' }}>
              <table className="w-full text-sm text-left">
                <thead style={{ backgroundColor: 'var(--color-bg)', borderBottom: `2px solid var(--color-border)` }}>
                  <tr>
                    {['User', 'Role', 'Status', 'Last Login', 'Joined', 'Auth', 'Actions'].map(col => (
                      <th key={col} className="px-5 py-4 font-semibold tracking-wide uppercase text-xs"
                        style={{ color: 'var(--color-text-subtle)' }}>{col}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y" style={{ borderColor: 'var(--color-border)' }}>
                  {users.length === 0 ? (
                    <tr><td colSpan={7} className="px-6 py-10 text-center text-sm"
                      style={{ color: 'var(--color-text-subtle)' }}>No users found</td></tr>
                  ) : users.map((user, i) => (
                    <tr key={user.id} style={{ backgroundColor: i % 2 === 0 ? 'transparent' : 'var(--color-bg)', opacity: user.enabled ? 1 : 0.6 }}>
                      <td className="px-5 py-3">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold text-white flex-shrink-0"
                            style={{ background: user.role === 'ADMIN' ? 'linear-gradient(135deg, var(--color-secondary), var(--color-accent))' : 'rgba(107,114,128,0.3)' }}>
                            {user.username.slice(0, 2).toUpperCase()}
                          </div>
                          <div>
                            <p className="font-semibold" style={{ color: 'var(--color-text)' }}>{user.username}</p>
                            {isSelf(user.username) && <p className="text-xs" style={{ color: 'var(--color-secondary)' }}>You</p>}
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-3"><RoleBadge role={user.role} /></td>
                      <td className="px-5 py-3"><StatusDot enabled={user.enabled} /></td>
                      <td className="px-5 py-3 text-sm" style={{ color: 'var(--color-text-subtle)' }}>{formatDate(user.lastLogin)}</td>
                      <td className="px-5 py-3 text-sm" style={{ color: 'var(--color-text-subtle)' }}>{formatDate(user.createdAt)}</td>
                      <td className="px-5 py-3 text-sm capitalize" style={{ color: 'var(--color-text-subtle)' }}>{user.provider ?? 'local'}</td>
                      <td className="px-5 py-3"><ActionButtons user={user} /></td>
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
                <div key={user.id} className="rounded-xl p-4 border"
                  style={{ backgroundColor: 'var(--color-surface)', borderColor: 'var(--color-border)', opacity: user.enabled ? 1 : 0.65 }}>
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold text-white"
                        style={{ background: user.role === 'ADMIN' ? 'linear-gradient(135deg, var(--color-secondary), var(--color-accent))' : 'rgba(107,114,128,0.3)' }}>
                        {user.username.slice(0, 2).toUpperCase()}
                      </div>
                      <div>
                        <p className="font-semibold text-sm" style={{ color: 'var(--color-text)' }}>{user.username}</p>
                        {isSelf(user.username) && <p className="text-xs" style={{ color: 'var(--color-secondary)' }}>You</p>}
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <RoleBadge role={user.role} />
                      <StatusDot enabled={user.enabled} />
                    </div>
                  </div>
                  <p className="text-xs mb-3" style={{ color: 'var(--color-text-subtle)' }}>
                    Joined {formatDate(user.createdAt)} · {user.provider ?? 'local'}
                    {user.lastLogin && ` · Last login ${formatDate(user.lastLogin)}`}
                  </p>
                  <ActionButtons user={user} />
                </div>
              ))}
            </div>

            {totalPages > 1 && (
              <div className="flex items-center justify-between gap-4 mt-6">
                <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
                  className="px-4 py-2 text-sm font-medium rounded-lg transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-md"
                  style={{ backgroundColor: 'var(--color-surface)', color: 'var(--color-primary)', border: `1px solid var(--color-border)` }}>
                  Previous
                </button>
                <span className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>
                  Page {page + 1} of {totalPages}
                </span>
                <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
                  className="px-4 py-2 text-sm font-medium rounded-lg transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-md"
                  style={{ backgroundColor: 'var(--color-surface)', color: 'var(--color-primary)', border: `1px solid var(--color-border)` }}>
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
