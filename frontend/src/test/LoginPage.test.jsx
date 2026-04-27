import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import LoginPage from '../components/LoginPage';
import { authService } from '../services/authService';

// Mock apiClient so no real HTTP requests are made
vi.mock('../services/apiClient', () => ({
  default: {
    post: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}));

import apiClient from '../services/apiClient';

function renderLoginPage() {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/dashboard" element={<div>Dashboard</div>} />
        <Route path="/register" element={<div>Register page</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders login form', () => {
    renderLoginPage();
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  it('shows register link', () => {
    renderLoginPage();
    expect(screen.getByRole('link', { name: /register/i })).toBeInTheDocument();
  });

  it('shows OAuth2 buttons', () => {
    renderLoginPage();
    expect(screen.getByText(/sign in with google/i)).toBeInTheDocument();
    expect(screen.getByText(/sign in with github/i)).toBeInTheDocument();
  });

  it('navigates to dashboard on successful login', async () => {
    apiClient.post.mockResolvedValueOnce({
      data: { token: 'tok', refreshToken: 'rt', username: 'alice', role: 'USER' },
    });

    renderLoginPage();
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'alice' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'secret' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => expect(screen.getByText('Dashboard')).toBeInTheDocument());
    expect(authService.getToken()).toBe('tok');
    expect(authService.getRefreshToken()).toBe('rt');
  });

  it('shows error on bad credentials', async () => {
    const err = { response: { status: 401 } };
    apiClient.post.mockRejectedValueOnce(err);

    renderLoginPage();
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'alice' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'bad' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => expect(screen.getByText(/invalid credentials/i)).toBeInTheDocument());
  });

  it('shows lock message on 423', async () => {
    const err = { response: { status: 423, data: { retryAfterSeconds: 900 } } };
    apiClient.post.mockRejectedValueOnce(err);

    renderLoginPage();
    fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'alice' } });
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'pw' } });
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => expect(screen.getByText(/locked/i)).toBeInTheDocument());
  });
});
