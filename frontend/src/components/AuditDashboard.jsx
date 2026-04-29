import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../services/apiClient';
import { authService } from '../services/authService';
import SkeletonLoader from './SkeletonLoader';

const PAGE_SIZE = 20;

export default function AuditDashboard() {
  const [requestedPage, setRequestedPage] = useState(0);
  const [loadedPage, setLoadedPage] = useState(-1);
  const [events, setEvents] = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [error, setError] = useState('');
  const [searchActor, setSearchActor] = useState('');
  const [searchAction, setSearchAction] = useState('');
  const [searchTarget, setSearchTarget] = useState('');
  const navigate = useNavigate();

  // loading is true when the requested page hasn't been loaded yet
  const loading = requestedPage !== loadedPage && !error;

  useEffect(() => {
    let cancelled = false;
    const params = new URLSearchParams({
      page: requestedPage,
      size: PAGE_SIZE,
    });
    if (searchActor) params.append('actor', searchActor);
    if (searchAction) params.append('action', searchAction);
    if (searchTarget) params.append('target', searchTarget);

    apiClient.get(`/api/v1/audit-events/recent?${params.toString()}`)
      .then((res) => {
        if (cancelled) return;
        setEvents(res.data.content);
        setTotalPages(res.data.totalPages);
        setTotalElements(res.data.totalElements);
        setError('');
        setLoadedPage(requestedPage);
      })
      .catch(() => {
        if (cancelled) return;
        setError('Failed to load audit events');
        setLoadedPage(requestedPage);
      });
    return () => { cancelled = true; };
  }, [requestedPage, searchActor, searchAction, searchTarget]);

  function handleClearFilters() {
    setSearchActor('');
    setSearchAction('');
    setSearchTarget('');
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
        <div className="flex flex-col sm:flex-row sm:items-baseline sm:justify-between gap-6 mb-8">
          <div>
            <h1 className="text-4xl sm:text-5xl font-bold mb-2" style={{ color: 'var(--color-primary)' }}>
              Audit Events
            </h1>
            {totalElements > 0 && (
              <p className="text-base" style={{ color: 'var(--color-text-subtle)' }}>
                {totalElements} total events
              </p>
            )}
          </div>
          <button
            onClick={handleLogout}
            className="w-full sm:w-auto px-6 py-2.5 rounded-lg font-semibold text-white transition-all duration-200 hover:shadow-xl hover:scale-105 active:scale-95 cursor-pointer"
            style={{
              backgroundColor: 'var(--color-accent)',
              fontSize: '0.875rem',
              letterSpacing: '0.025em',
            }}
          >
            Logout
          </button>
        </div>

        {/* Search and Filter Section */}
        <div className="mb-8 grid grid-cols-1 sm:grid-cols-3 gap-4">
          <input
            type="text"
            placeholder="Filter by actor..."
            value={searchActor}
            onChange={(e) => {
              setSearchActor(e.target.value);
              setRequestedPage(0);
            }}
            className="px-4 py-3 rounded-lg text-sm font-medium border-2 transition-all duration-200 focus:outline-none placeholder-gray-400"
            style={{
              backgroundColor: 'var(--color-surface)',
              color: 'var(--color-text)',
              borderColor: searchActor ? 'var(--color-secondary)' : 'var(--color-border)',
              boxShadow: searchActor ? '0 0 0 3px rgba(99, 102, 241, 0.1)' : 'none',
            }}
          />
          <input
            type="text"
            placeholder="Filter by action..."
            value={searchAction}
            onChange={(e) => {
              setSearchAction(e.target.value);
              setRequestedPage(0);
            }}
            className="px-4 py-3 rounded-lg text-sm font-medium border-2 transition-all duration-200 focus:outline-none placeholder-gray-400"
            style={{
              backgroundColor: 'var(--color-surface)',
              color: 'var(--color-text)',
              borderColor: searchAction ? 'var(--color-secondary)' : 'var(--color-border)',
              boxShadow: searchAction ? '0 0 0 3px rgba(99, 102, 241, 0.1)' : 'none',
            }}
          />
          <input
            type="text"
            placeholder="Filter by target..."
            value={searchTarget}
            onChange={(e) => {
              setSearchTarget(e.target.value);
              setRequestedPage(0);
            }}
            className="px-4 py-3 rounded-lg text-sm font-medium border-2 transition-all duration-200 focus:outline-none placeholder-gray-400"
            style={{
              backgroundColor: 'var(--color-surface)',
              color: 'var(--color-text)',
              borderColor: searchTarget ? 'var(--color-secondary)' : 'var(--color-border)',
              boxShadow: searchTarget ? '0 0 0 3px rgba(99, 102, 241, 0.1)' : 'none',
            }}
          />
        </div>

        {(searchActor || searchAction || searchTarget) && (
          <div className="mb-6 p-4 rounded-lg border-2 flex items-center justify-between animate-in fade-in slide-in-from-top-2 duration-300" style={{ borderColor: 'var(--color-secondary)', backgroundColor: 'rgba(99, 102, 241, 0.05)' }}>
            <p className="text-sm font-semibold" style={{ color: 'var(--color-secondary)' }}>
              {[searchActor, searchAction, searchTarget].filter(Boolean).length} filter{[searchActor, searchAction, searchTarget].filter(Boolean).length !== 1 ? 's' : ''} active
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

        {loading && <SkeletonLoader count={PAGE_SIZE} />}
        {error && (
          <div className="p-4 rounded-lg" style={{ backgroundColor: '#fee2e2', color: '#991b1b' }}>
            {error}
          </div>
        )}

        {!loading && !error && (
          <>
            {/* Table view for md+ */}
            <div className="hidden md:block rounded-lg overflow-hidden border transition-all duration-300" style={{ borderColor: 'var(--color-border)', backgroundColor: 'var(--color-surface)' }}>
              <table className="w-full text-sm text-left">
                <thead style={{ backgroundColor: 'var(--color-bg)', borderBottom: `2px solid var(--color-border)` }}>
                  <tr>
                    <th className="px-6 py-4 font-semibold tracking-wide uppercase text-xs" style={{ color: 'var(--color-text-subtle)' }}>Timestamp</th>
                    <th className="px-6 py-4 font-semibold tracking-wide uppercase text-xs" style={{ color: 'var(--color-text-subtle)' }}>Actor</th>
                    <th className="px-6 py-4 font-semibold tracking-wide uppercase text-xs" style={{ color: 'var(--color-text-subtle)' }}>Action</th>
                    <th className="px-6 py-4 font-semibold tracking-wide uppercase text-xs" style={{ color: 'var(--color-text-subtle)' }}>Target</th>
                  </tr>
                </thead>
                <tbody className="divide-y" style={{ borderColor: 'var(--color-border)' }}>
                  {events.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="px-6 py-8 text-center" style={{ color: 'var(--color-text-subtle)' }}>
                        No audit events found
                      </td>
                    </tr>
                  ) : (
                    events.map((ev, i) => (
                      <tr key={ev.id} className="transition-all duration-200 hover:shadow-sm" style={{ backgroundColor: i % 2 === 0 ? 'transparent' : 'var(--color-bg)', transform: 'translateZ(0)' }}>
                        <td className="px-6 py-4 transition-colors duration-200" style={{ color: 'var(--color-text-subtle)' }}>
                          {new Date(ev.eventTime).toLocaleString()}
                        </td>
                        <td className="px-6 py-4 font-medium transition-colors duration-200">{ev.actor}</td>
                        <td className="px-6 py-4 transition-colors duration-200" style={{ color: 'var(--color-text)' }}>{ev.action}</td>
                        <td className="px-6 py-4 font-mono text-xs transition-colors duration-200" style={{ color: 'var(--color-secondary)' }}>{ev.target}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Card view for sm and md */}
            <div className="md:hidden space-y-3">
              {events.length === 0 ? (
                <div className="text-center py-8" style={{ color: 'var(--color-text-subtle)' }}>
                  No audit events found
                </div>
              ) : (
                events.map((ev, i) => (
                  <div key={ev.id} className="rounded-lg p-4 space-y-3 border-2 transition-all duration-200 hover:shadow-lg active:scale-95 cursor-pointer" style={{ backgroundColor: 'var(--color-surface)', borderColor: 'var(--color-border)', animationDelay: `${i * 30}ms`, transform: 'translateZ(0)' }}>
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-wide mb-2" style={{ color: 'var(--color-text-subtle)' }}>Timestamp</p>
                      <p className="text-sm font-medium" style={{ color: 'var(--color-text)' }}>
                        {new Date(ev.eventTime).toLocaleString()}
                      </p>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-wide mb-2" style={{ color: 'var(--color-text-subtle)' }}>Actor</p>
                        <p className="text-sm font-semibold" style={{ color: 'var(--color-text)' }}>{ev.actor}</p>
                      </div>
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-wide mb-2" style={{ color: 'var(--color-text-subtle)' }}>Action</p>
                        <p className="text-sm font-medium" style={{ color: 'var(--color-text)' }}>{ev.action}</p>
                      </div>
                    </div>
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-wide mb-2" style={{ color: 'var(--color-text-subtle)' }}>Target</p>
                      <p className="text-sm font-mono break-words" style={{ color: 'var(--color-secondary)' }}>{ev.target}</p>
                    </div>
                  </div>
                ))
              )}
            </div>

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
