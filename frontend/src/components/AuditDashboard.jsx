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
  const navigate = useNavigate();

  // loading is true when the requested page hasn't been loaded yet
  const loading = requestedPage !== loadedPage && !error;

  useEffect(() => {
    let cancelled = false;
    apiClient.get(`/api/v1/audit-events/recent?page=${requestedPage}&size=${PAGE_SIZE}`)
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
  }, [requestedPage]);

  function handleLogout() {
    apiClient.post('/api/v1/auth/logout').finally(() => {
      authService.logout();
      navigate('/login');
    });
  }

  return (
    <div className="min-h-screen bg-gray-50 p-4 sm:p-6">
      <div className="max-w-5xl mx-auto">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-800">Audit Events</h1>
            {totalElements > 0 && (
              <p className="text-sm text-gray-500">{totalElements} total events</p>
            )}
          </div>
          <button
            onClick={handleLogout}
            className="w-full sm:w-auto bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 transition text-sm"
          >
            Logout
          </button>
        </div>

        {loading && <SkeletonLoader count={PAGE_SIZE} />}
        {error && <p className="text-red-600">{error}</p>}

        {!loading && !error && (
          <>
            {/* Table view for md+ */}
            <div className="hidden md:block bg-white rounded-lg shadow overflow-hidden">
              <table className="w-full text-sm text-left">
                <thead className="bg-gray-100 text-gray-600 uppercase text-xs">
                  <tr>
                    <th className="px-4 py-3">Timestamp</th>
                    <th className="px-4 py-3">Actor</th>
                    <th className="px-4 py-3">Action</th>
                    <th className="px-4 py-3">Target</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-100">
                  {events.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="px-4 py-6 text-center text-gray-400">
                        No audit events found
                      </td>
                    </tr>
                  ) : (
                    events.map((ev) => (
                      <tr key={ev.id} className="hover:bg-gray-50">
                        <td className="px-4 py-3 text-gray-500">
                          {new Date(ev.eventTime).toLocaleString()}
                        </td>
                        <td className="px-4 py-3 font-medium text-gray-800">{ev.actor}</td>
                        <td className="px-4 py-3 text-gray-600">{ev.action}</td>
                        <td className="px-4 py-3 text-gray-600">{ev.target}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Card view for sm and md */}
            <div className="md:hidden space-y-3">
              {events.length === 0 ? (
                <div className="text-center text-gray-400 py-8">
                  No audit events found
                </div>
              ) : (
                events.map((ev) => (
                  <div key={ev.id} className="bg-white rounded-lg shadow p-4 space-y-2">
                    <div>
                      <p className="text-xs text-gray-500">Timestamp</p>
                      <p className="text-sm font-medium text-gray-800">
                        {new Date(ev.eventTime).toLocaleString()}
                      </p>
                    </div>
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-xs text-gray-500">Actor</p>
                        <p className="text-sm font-medium text-gray-800">{ev.actor}</p>
                      </div>
                      <div>
                        <p className="text-xs text-gray-500">Action</p>
                        <p className="text-sm text-gray-600">{ev.action}</p>
                      </div>
                    </div>
                    <div>
                      <p className="text-xs text-gray-500">Target</p>
                      <p className="text-sm text-gray-600 break-words">{ev.target}</p>
                    </div>
                  </div>
                ))
              )}
            </div>

            {totalPages > 1 && (
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mt-4">
                <button
                  onClick={() => setRequestedPage((p) => Math.max(0, p - 1))}
                  disabled={requestedPage === 0}
                  className="w-full sm:w-auto px-4 py-2 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <span className="text-sm text-gray-600 text-center">
                  Page {requestedPage + 1} of {totalPages}
                </span>
                <button
                  onClick={() => setRequestedPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={requestedPage >= totalPages - 1}
                  className="w-full sm:w-auto px-4 py-2 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
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
