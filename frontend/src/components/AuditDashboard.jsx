import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../services/apiClient';
import { authService } from '../services/authService';
import SkeletonLoader from './SkeletonLoader';

const PAGE_SIZE = 20;

const ACTION_CONFIG = {
  login:             { bg: 'rgba(16,185,129,0.1)',   color: '#059669' },
  register:          { bg: 'rgba(16,185,129,0.1)',   color: '#059669' },
  logout:            { bg: 'rgba(107,114,128,0.12)', color: '#6b7280' },
  permission_denied: { bg: 'rgba(239,68,68,0.1)',    color: '#dc2626' },
  unauthorized:      { bg: 'rgba(239,68,68,0.1)',    color: '#dc2626' },
  forbidden:         { bg: 'rgba(239,68,68,0.1)',    color: '#dc2626' },
  http_request:      { bg: 'rgba(99,102,241,0.1)',   color: '#6366f1' },
  token_refresh:     { bg: 'rgba(245,158,11,0.1)',   color: '#d97706' },
};

function ActionBadge({ action }) {
  const cfg = ACTION_CONFIG[action?.toLowerCase()] ?? {
    bg: 'rgba(107,114,128,0.06)',
    color: 'var(--color-text-subtle)',
  };
  return (
    <span
      className="inline-flex items-center px-2 py-0.5 rounded font-mono text-xs font-semibold tracking-wide whitespace-nowrap"
      style={{ backgroundColor: cfg.bg, color: cfg.color }}
    >
      {action}
    </span>
  );
}

function formatRelativeTime(isoString) {
  const date = new Date(isoString);
  const diffSec = Math.floor((Date.now() - date.getTime()) / 1000);
  if (diffSec < 60) return 'just now';
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHr = Math.floor(diffMin / 60);
  if (diffHr < 24) return `${diffHr}h ago`;
  const diffDay = Math.floor(diffHr / 24);
  if (diffDay < 7) return `${diffDay}d ago`;
  return date.toLocaleDateString();
}

function EmptyState({ hasFilters, onClear }) {
  if (hasFilters) {
    return (
      <div className="flex flex-col items-center justify-center py-16 space-y-4">
        <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" style={{ color: 'var(--color-border)' }}>
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
          <line x1="8" y1="11" x2="14" y2="11" />
        </svg>
        <div className="text-center">
          <p className="text-sm font-semibold mb-1" style={{ color: 'var(--color-text)' }}>No results for current filters</p>
          <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>Try adjusting your search criteria</p>
        </div>
        <button
          onClick={onClear}
          className="text-xs font-semibold px-3 py-1.5 rounded-lg transition-all duration-200 hover:opacity-80"
          style={{ color: 'var(--color-secondary)', backgroundColor: 'rgba(99,102,241,0.08)' }}
        >
          Reset filters
        </button>
      </div>
    );
  }
  return (
    <div className="flex flex-col items-center justify-center py-16 space-y-4">
      <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true" style={{ color: 'var(--color-border)' }}>
        <path d="M22 12h-6l-2 3h-4l-2-3H2" />
        <path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z" />
      </svg>
      <div className="text-center">
        <p className="text-sm font-semibold mb-1" style={{ color: 'var(--color-text)' }}>No audit events found</p>
        <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>Events will appear here as they are recorded</p>
      </div>
    </div>
  );
}

const COLS = ['Timestamp', 'Actor', 'Action', 'Target'];

const INITIAL_RESULT = { key: null, events: [], totalPages: 0, totalElements: 0, error: '' };

export default function AuditDashboard() {
  const [requestedPage, setRequestedPage] = useState(0);
  const [retryKey, setRetryKey] = useState(0);
  const [searchActor, setSearchActor] = useState('');
  const [searchAction, setSearchAction] = useState('');
  const [searchTarget, setSearchTarget] = useState('');
  const [debouncedActor, setDebouncedActor] = useState('');
  const [debouncedAction, setDebouncedAction] = useState('');
  const [debouncedTarget, setDebouncedTarget] = useState('');
  // Single state object updated only inside async callbacks — satisfies react-hooks/set-state-in-effect
  const [fetchResult, setFetchResult] = useState(INITIAL_RESULT);
  const navigate = useNavigate();

  const fetchKey = `${requestedPage}|${debouncedActor}|${debouncedAction}|${debouncedTarget}|${retryKey}`;
  const isLoading = fetchKey !== fetchResult.key;
  const events       = isLoading ? [] : fetchResult.events;
  const totalPages   = isLoading ? 0  : fetchResult.totalPages;
  const totalElements = isLoading ? 0 : fetchResult.totalElements;
  const error        = isLoading ? '' : fetchResult.error;
  const hasFilters   = !!(searchActor || searchAction || searchTarget);

  useEffect(() => {
    const t = setTimeout(() => setDebouncedActor(searchActor), 300);
    return () => clearTimeout(t);
  }, [searchActor]);

  useEffect(() => {
    const t = setTimeout(() => setDebouncedAction(searchAction), 300);
    return () => clearTimeout(t);
  }, [searchAction]);

  useEffect(() => {
    const t = setTimeout(() => setDebouncedTarget(searchTarget), 300);
    return () => clearTimeout(t);
  }, [searchTarget]);

  useEffect(() => {
    let cancelled = false;
    const key = `${requestedPage}|${debouncedActor}|${debouncedAction}|${debouncedTarget}|${retryKey}`;

    const params = new URLSearchParams({ page: requestedPage, size: PAGE_SIZE });
    if (debouncedActor)  params.append('actor',  debouncedActor);
    if (debouncedAction) params.append('action', debouncedAction);
    if (debouncedTarget) params.append('target', debouncedTarget);

    apiClient.get(`/api/v1/audit-events/recent?${params.toString()}`)
      .then((res) => {
        if (cancelled) return;
        setFetchResult({
          key,
          events: res.data.content,
          totalPages: res.data.totalPages,
          totalElements: res.data.totalElements,
          error: '',
        });
      })
      .catch((err) => {
        if (cancelled) return;
        const msg = err?.response?.status === 429
          ? 'Too many requests — slow down and retry'
          : 'Failed to load audit events';
        setFetchResult({ key, events: [], totalPages: 0, totalElements: 0, error: msg });
      });
    return () => { cancelled = true; };
  }, [requestedPage, debouncedActor, debouncedAction, debouncedTarget, retryKey]);

  function handleClearFilters() {
    setSearchActor('');
    setSearchAction('');
    setSearchTarget('');
    setDebouncedActor('');
    setDebouncedAction('');
    setDebouncedTarget('');
    setRequestedPage(0);
  }

  function handleLogout() {
    apiClient.post('/api/v1/auth/logout').finally(() => {
      authService.logout();
      navigate('/login');
    });
  }

  return (
    <div className="min-h-screen" style={{ backgroundColor: 'var(--color-bg)' }}>
      <div className="max-w-5xl mx-auto p-4 sm:p-8">

        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-baseline sm:justify-between gap-6 mb-8">
          <div>
            <h1 className="text-4xl sm:text-5xl font-bold mb-2" style={{ color: 'var(--color-primary)' }}>
              Audit Events
            </h1>
            {totalElements > 0 && (
              <p className="text-base" style={{ color: 'var(--color-text-subtle)' }}>
                {totalElements.toLocaleString()} total events
              </p>
            )}
          </div>
          <button
            onClick={handleLogout}
            className="w-full sm:w-auto px-6 py-2.5 rounded-lg font-semibold text-white transition-all duration-200 hover:shadow-xl hover:scale-105 active:scale-95 cursor-pointer"
            style={{ backgroundColor: 'var(--color-accent)', fontSize: '0.875rem', letterSpacing: '0.025em' }}
          >
            Logout
          </button>
        </div>

        {/* Filters */}
        <div className="mb-8 grid grid-cols-1 sm:grid-cols-3 gap-4">
          {[
            { placeholder: 'Filter by actor...',  value: searchActor,  set: setSearchActor },
            { placeholder: 'Filter by action...', value: searchAction, set: setSearchAction },
            { placeholder: 'Filter by target...', value: searchTarget, set: setSearchTarget },
          ].map(({ placeholder, value, set }) => (
            <input
              key={placeholder}
              type="text"
              placeholder={placeholder}
              value={value}
              onChange={(e) => { set(e.target.value); setRequestedPage(0); }}
              className="px-4 py-3 rounded-lg text-sm font-medium border-2 transition-all duration-200 focus:outline-none placeholder-gray-400"
              style={{
                backgroundColor: 'var(--color-surface)',
                color: 'var(--color-text)',
                borderColor: value ? 'var(--color-secondary)' : 'var(--color-border)',
                boxShadow: value ? '0 0 0 3px rgba(99,102,241,0.1)' : 'none',
              }}
            />
          ))}
        </div>

        {/* Active filter banner */}
        {hasFilters && (
          <div
            className="mb-6 p-4 rounded-lg border-2 flex items-center justify-between animate-in fade-in slide-in-from-top-2 duration-300"
            style={{ borderColor: 'var(--color-secondary)', backgroundColor: 'rgba(99,102,241,0.05)' }}
          >
            <p className="text-sm font-semibold" style={{ color: 'var(--color-secondary)' }}>
              {[searchActor, searchAction, searchTarget].filter(Boolean).length} filter
              {[searchActor, searchAction, searchTarget].filter(Boolean).length !== 1 ? 's' : ''} active
            </p>
            <button
              onClick={handleClearFilters}
              className="text-sm font-semibold px-4 py-2 rounded-lg transition-all duration-200 hover:shadow-md active:scale-95"
              style={{ color: 'white', backgroundColor: 'var(--color-secondary)' }}
            >
              Clear
            </button>
          </div>
        )}

        {/* Loading */}
        {isLoading && <SkeletonLoader count={PAGE_SIZE} />}

        {/* Error */}
        {!isLoading && error && (
          <div
            className="flex flex-col items-center justify-center py-16 space-y-4 rounded-lg border"
            style={{ borderColor: 'rgba(239,68,68,0.2)', backgroundColor: 'rgba(239,68,68,0.04)' }}
          >
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#dc2626" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <div className="text-center">
              <p className="text-sm font-semibold mb-1" style={{ color: '#dc2626' }}>Failed to load audit events</p>
              <p className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>Check your connection and try again</p>
            </div>
            <button
              onClick={() => setRetryKey((k) => k + 1)}
              className="text-xs font-semibold px-4 py-2 rounded-lg transition-all duration-200 hover:opacity-80 active:scale-95"
              style={{ color: 'white', backgroundColor: '#dc2626' }}
            >
              Retry
            </button>
          </div>
        )}

        {/* Content */}
        {!isLoading && !error && (
          <>
            {/* Table — md+ */}
            <div
              className="hidden md:block rounded-lg overflow-hidden border transition-all duration-300"
              style={{ borderColor: 'var(--color-border)', backgroundColor: 'var(--color-surface)' }}
            >
              <table className="w-full text-sm text-left">
                <thead style={{ backgroundColor: 'var(--color-bg)', borderBottom: `2px solid var(--color-border)` }}>
                  <tr>
                    {COLS.map((col) => (
                      <th key={col} className="px-6 py-4 font-semibold tracking-wide uppercase text-xs" style={{ color: 'var(--color-text-subtle)' }}>
                        {col}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y" style={{ borderColor: 'var(--color-border)' }}>
                  {events.length === 0 ? (
                    <tr>
                      <td colSpan={4}>
                        <EmptyState hasFilters={hasFilters} onClear={handleClearFilters} />
                      </td>
                    </tr>
                  ) : (
                    events.map((ev, i) => (
                      <tr
                        key={ev.id}
                        className="transition-all duration-200 hover:shadow-sm"
                        style={{ backgroundColor: i % 2 === 0 ? 'transparent' : 'var(--color-bg)', transform: 'translateZ(0)' }}
                      >
                        <td className="px-6 py-4" style={{ color: 'var(--color-text-subtle)' }}>
                          <span title={new Date(ev.eventTime).toISOString()}>
                            {formatRelativeTime(ev.eventTime)}
                          </span>
                        </td>
                        <td className="px-6 py-4 font-medium" style={{ color: 'var(--color-text)' }}>
                          {ev.actor}
                        </td>
                        <td className="px-6 py-4">
                          <ActionBadge action={ev.action} />
                        </td>
                        <td className="px-6 py-4 font-mono text-xs" style={{ color: 'var(--color-secondary)' }}>
                          {ev.target}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Cards — mobile */}
            <div className="md:hidden space-y-3">
              {events.length === 0 ? (
                <EmptyState hasFilters={hasFilters} onClear={handleClearFilters} />
              ) : (
                events.map((ev, i) => (
                  <div
                    key={ev.id}
                    className="rounded-lg p-4 space-y-3 border-2 transition-all duration-200 hover:shadow-lg active:scale-95 cursor-pointer"
                    style={{ backgroundColor: 'var(--color-surface)', borderColor: 'var(--color-border)', animationDelay: `${i * 30}ms`, transform: 'translateZ(0)' }}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span title={new Date(ev.eventTime).toISOString()} className="text-xs" style={{ color: 'var(--color-text-subtle)' }}>
                        {formatRelativeTime(ev.eventTime)}
                      </span>
                      <ActionBadge action={ev.action} />
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-wide mb-1" style={{ color: 'var(--color-text-subtle)' }}>Actor</p>
                        <p className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>{ev.actor}</p>
                      </div>
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-wide mb-1" style={{ color: 'var(--color-text-subtle)' }}>Target</p>
                        <p className="text-sm font-mono break-words" style={{ color: 'var(--color-secondary)' }}>{ev.target}</p>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mt-8">
                <button
                  onClick={() => setRequestedPage((p) => Math.max(0, p - 1))}
                  disabled={requestedPage === 0}
                  className="w-full sm:w-auto px-4 py-2 text-sm font-medium rounded transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-md active:scale-95"
                  style={{ backgroundColor: 'var(--color-surface)', color: 'var(--color-primary)', border: `1px solid var(--color-border)` }}
                >
                  Previous
                </button>
                <span className="text-sm text-center font-medium" style={{ color: 'var(--color-text-subtle)' }}>
                  Page {requestedPage + 1} of {totalPages}
                </span>
                <button
                  onClick={() => setRequestedPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={requestedPage >= totalPages - 1}
                  className="w-full sm:w-auto px-4 py-2 text-sm font-medium rounded transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-md active:scale-95"
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
