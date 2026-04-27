import { BrowserRouter, Routes, Route, Navigate, useSearchParams } from 'react-router-dom';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import LoginPage from './components/LoginPage';
import AuditDashboard from './components/AuditDashboard';
import ProtectedRoute from './components/ProtectedRoute';
import { authService } from './services/authService';
import './index.css';

function OAuth2Redirect() {
  const [params] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const token = params.get('token');
    if (token) {
      authService.saveToken(token);
      navigate('/dashboard', { replace: true });
    } else {
      navigate('/login', { replace: true });
    }
  }, []);

  return <p className="text-center mt-10 text-gray-500">Completing login...</p>;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/oauth2/redirect" element={<OAuth2Redirect />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <AuditDashboard />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
