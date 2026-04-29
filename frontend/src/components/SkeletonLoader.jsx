export default function SkeletonLoader({ count = 5 }) {
  return (
    <>
      {/* Skeleton table for md+ */}
      <div
        className="hidden md:block rounded-lg overflow-hidden border"
        style={{ backgroundColor: 'var(--color-surface)', borderColor: 'var(--color-border)' }}
      >
        <table className="w-full text-sm text-left">
          <thead style={{ backgroundColor: 'var(--color-bg)', borderBottom: `2px solid var(--color-border)` }}>
            <tr>
              <th className="px-6 py-4 font-semibold tracking-wide uppercase text-xs" style={{ color: 'var(--color-text-subtle)' }}>Timestamp</th>
              <th className="px-6 py-4 font-semibold tracking-wide uppercase text-xs" style={{ color: 'var(--color-text-subtle)' }}>Actor</th>
              <th className="px-6 py-4 font-semibold tracking-wide uppercase text-xs" style={{ color: 'var(--color-text-subtle)' }}>Action</th>
              <th className="px-6 py-4 font-semibold tracking-wide uppercase text-xs" style={{ color: 'var(--color-text-subtle)' }}>Target</th>
            </tr>
          </thead>
          <tbody>
            {Array.from({ length: count }).map((_, i) => (
              <tr
                key={i}
                style={{ borderTop: i > 0 ? `1px solid var(--color-border)` : undefined }}
              >
                <td className="px-6 py-4">
                  <div className="h-4 rounded w-32 animate-pulse" style={{ backgroundColor: 'var(--color-border)' }} />
                </td>
                <td className="px-6 py-4">
                  <div className="h-4 rounded w-20 animate-pulse" style={{ backgroundColor: 'var(--color-border)' }} />
                </td>
                <td className="px-6 py-4">
                  <div className="h-4 rounded w-24 animate-pulse" style={{ backgroundColor: 'var(--color-border)' }} />
                </td>
                <td className="px-6 py-4">
                  <div className="h-4 rounded w-28 animate-pulse" style={{ backgroundColor: 'var(--color-border)' }} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Skeleton cards for mobile */}
      <div className="md:hidden space-y-3">
        {Array.from({ length: count }).map((_, i) => (
          <div
            key={i}
            className="rounded-lg p-4 space-y-3 border"
            style={{ backgroundColor: 'var(--color-surface)', borderColor: 'var(--color-border)' }}
          >
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide mb-2" style={{ color: 'var(--color-text-subtle)' }}>Timestamp</p>
              <div className="h-4 rounded w-48 animate-pulse" style={{ backgroundColor: 'var(--color-border)' }} />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide mb-2" style={{ color: 'var(--color-text-subtle)' }}>Actor</p>
                <div className="h-4 rounded w-20 animate-pulse" style={{ backgroundColor: 'var(--color-border)' }} />
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide mb-2" style={{ color: 'var(--color-text-subtle)' }}>Action</p>
                <div className="h-4 rounded w-24 animate-pulse" style={{ backgroundColor: 'var(--color-border)' }} />
              </div>
            </div>
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide mb-2" style={{ color: 'var(--color-text-subtle)' }}>Target</p>
              <div className="h-4 rounded w-32 animate-pulse" style={{ backgroundColor: 'var(--color-border)' }} />
            </div>
          </div>
        ))}
      </div>
    </>
  );
}
