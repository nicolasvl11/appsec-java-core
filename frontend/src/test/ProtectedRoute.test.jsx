import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from '../components/ProtectedRoute';
import { authService } from '../services/authService';

const createValidJWT = (expiresIn = 3600) => {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const now = Math.floor(Date.now() / 1000);
  const payload = btoa(JSON.stringify({ sub: 'test', exp: now + expiresIn }));
  const signature = 'test-sig';
  return `${header}.${payload}.${signature}`;
};

function TestApp({ initialPath }) {
  return (
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/login" element={<div>Login page</div>} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <div>Dashboard</div>
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>
  );
}

describe('ProtectedRoute', () => {
  beforeEach(() => localStorage.clear());

  it('renders children when authenticated', () => {
    authService.saveToken(createValidJWT());
    render(<TestApp initialPath="/dashboard" />);
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });

  it('redirects to /login when not authenticated', () => {
    render(<TestApp initialPath="/dashboard" />);
    expect(screen.getByText('Login page')).toBeInTheDocument();
  });
});
