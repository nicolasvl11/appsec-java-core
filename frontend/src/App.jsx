import { BrowserRouter, Routes, Route, Navigate, useSearchParams, useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import LoginPage from './components/LoginPage';
import RegisterPage from './components/RegisterPage';
import AuditDashboard from './components/AuditDashboard';
import ProfilePage from './components/ProfilePage';
import AdminPage from './components/AdminPage';
import ProtectedRoute from './components/ProtectedRoute';
import { authService } from './services/authService';

function OAuth2Redirect() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const token = params.get('token');
    const refreshToken = params.get('refreshToken');
    if (token) {
      authService.saveToken(token);
      if (refreshToken) authService.saveRefreshToken(refreshToken);
      navigate('/dashboard', { replace: true });
    } else {
      navigate('/login', { replace: true });
    }
  }, [navigate, params]);

  return (
    <div
      className="min-h-screen flex items-center justify-center"
      style={{ backgroundColor: 'var(--color-bg)' }}
    >
      <div className="flex flex-col items-center gap-4">
        <svg className="animate-spin" width="28" height="28" viewBox="0 0 24 24" fill="none" aria-hidden="true" style={{ color: 'var(--color-secondary)' }}>
          <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" strokeOpacity="0.25" />
          <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
        </svg>
        <p className="text-sm font-medium" style={{ color: 'var(--color-text-subtle)' }}>
          Completing login…
        </p>
      </div>
    </div>
  );
}

function AdminRoute({ children }) {
  if (!authService.isAuthenticated()) return <Navigate to="/login" replace />;
  if (!authService.isAdmin()) return <Navigate to="/dashboard" replace />;
  return children;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/oauth2/redirect" element={<OAuth2Redirect />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <AuditDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={
            <AdminRoute>
              <AdminPage />
            </AdminRoute>
          }
        />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
