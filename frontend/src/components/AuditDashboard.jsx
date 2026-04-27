import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../services/apiClient';
import { authService } from '../services/authService';

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
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-gray-800">Audit Events</h1>
            {totalElements > 0 && (
              <p className="text-sm text-gray-500">{totalElements} total events</p>
            )}
          </div>
          <button
            onClick={handleLogout}
            className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 transition text-sm"
          >
            Logout
          </button>
        </div>

        {loading && <p className="text-gray-500">Loading...</p>}
        {error && <p className="text-red-600">{error}</p>}

        {!loading && !error && (
          <>
            <div className="bg-white rounded-lg shadow overflow-hidden">
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

            {totalPages > 1 && (
              <div className="flex items-center justify-between mt-4">
                <button
                  onClick={() => setRequestedPage((p) => Math.max(0, p - 1))}
                  disabled={requestedPage === 0}
                  className="px-4 py-2 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  Previous
                </button>
                <span className="text-sm text-gray-600">
                  Page {requestedPage + 1} of {totalPages}
                </span>
                <button
                  onClick={() => setRequestedPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={requestedPage >= totalPages - 1}
                  className="px-4 py-2 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed"
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
