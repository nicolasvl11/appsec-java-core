export default function SkeletonLoader({ count = 5 }) {
  return (
    <>
      {/* Skeleton table for md+ */}
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
            {Array.from({ length: count }).map((_, i) => (
              <tr key={i} className="hover:bg-gray-50">
                <td className="px-4 py-3">
                  <div className="h-4 bg-gray-200 rounded w-32 animate-pulse" />
                </td>
                <td className="px-4 py-3">
                  <div className="h-4 bg-gray-200 rounded w-20 animate-pulse" />
                </td>
                <td className="px-4 py-3">
                  <div className="h-4 bg-gray-200 rounded w-24 animate-pulse" />
                </td>
                <td className="px-4 py-3">
                  <div className="h-4 bg-gray-200 rounded w-28 animate-pulse" />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Skeleton cards for sm and md */}
      <div className="md:hidden space-y-3">
        {Array.from({ length: count }).map((_, i) => (
          <div key={i} className="bg-white rounded-lg shadow p-4 space-y-2">
            <div>
              <p className="text-xs text-gray-500 mb-1">Timestamp</p>
              <div className="h-4 bg-gray-200 rounded w-48 animate-pulse" />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-xs text-gray-500 mb-1">Actor</p>
                <div className="h-4 bg-gray-200 rounded w-20 animate-pulse" />
              </div>
              <div>
                <p className="text-xs text-gray-500 mb-1">Action</p>
                <div className="h-4 bg-gray-200 rounded w-24 animate-pulse" />
              </div>
            </div>
            <div>
              <p className="text-xs text-gray-500 mb-1">Target</p>
              <div className="h-4 bg-gray-200 rounded w-32 animate-pulse" />
            </div>
          </div>
        ))}
      </div>
    </>
  );
}
