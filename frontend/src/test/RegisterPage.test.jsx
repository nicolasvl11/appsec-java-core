import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import RegisterPage from '../components/RegisterPage';
import { authService } from '../services/authService';

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

function renderRegisterPage() {
  return render(
    <MemoryRouter initialEntries={['/register']}>
      <Routes>
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/dashboard" element={<div>Dashboard</div>} />
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>
    </MemoryRouter>
  );
}

function fillForm(username, password, confirm) {
  fireEvent.change(screen.getByLabelText('Username'), { target: { value: username } });
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: password } });
  fireEvent.change(screen.getByLabelText('Confirm password'), { target: { value: confirm } });
}

describe('RegisterPage', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('renders register form', () => {
    renderRegisterPage();
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /register/i })).toBeInTheDocument();
  });

  it('shows sign-in link', () => {
    renderRegisterPage();
    expect(screen.getByRole('link', { name: /sign in/i })).toBeInTheDocument();
  });

  it('shows error when passwords do not match', async () => {
    renderRegisterPage();
    fillForm('alice', 'password1', 'different');
    fireEvent.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() =>
      expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument()
    );
  });

  it('navigates to dashboard on successful registration', async () => {
    apiClient.post.mockResolvedValueOnce({
      data: { token: 'tok', refreshToken: 'rt', username: 'bob', role: 'USER' },
    });

    renderRegisterPage();
    fillForm('bob', 'password123', 'password123');
    fireEvent.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() => expect(screen.getByText('Dashboard')).toBeInTheDocument());
    expect(authService.getToken()).toBe('tok');
  });

  it('shows server error message on conflict', async () => {
    const err = { response: { data: { detail: 'Username already taken' } } };
    apiClient.post.mockRejectedValueOnce(err);

    renderRegisterPage();
    fillForm('alice', 'password123', 'password123');
    fireEvent.click(screen.getByRole('button', { name: /register/i }));

    await waitFor(() =>
      expect(screen.getByText(/username already taken/i)).toBeInTheDocument()
    );
  });
});
