import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ProfilePage from '../components/ProfilePage';

vi.mock('../services/apiClient', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}));

import apiClient from '../services/apiClient';

const LOCAL_PROFILE = {
  id: 1,
  username: 'alice',
  role: 'USER',
  createdAt: '2024-01-15T10:00:00Z',
  email: null,
  provider: null,
  lastLogin: '2024-05-01T08:30:00Z',
};

const OAUTH_PROFILE = {
  ...LOCAL_PROFILE,
  email: 'alice@gmail.com',
  provider: 'google',
};

function renderProfile() {
  return render(
    <MemoryRouter initialEntries={['/profile']}>
      <Routes>
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/dashboard" element={<div>Dashboard</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows skeleton while loading', () => {
    apiClient.get.mockReturnValueOnce(new Promise(() => {}));
    renderProfile();
    expect(document.querySelectorAll('.animate-pulse').length).toBeGreaterThan(0);
  });

  it('renders local user profile', async () => {
    apiClient.get.mockResolvedValueOnce({ data: LOCAL_PROFILE });
    renderProfile();

    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument());
    expect(screen.getByText('USER')).toBeInTheDocument();
    expect(screen.getByText('#1')).toBeInTheDocument();
    expect(screen.getByText(/local account/i)).toBeInTheDocument();
  });

  it('renders oauth2 user profile with email', async () => {
    apiClient.get.mockResolvedValueOnce({ data: OAUTH_PROFILE });
    renderProfile();

    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument());
    expect(screen.getByText('alice@gmail.com')).toBeInTheDocument();
    expect(screen.getAllByText(/google/i).length).toBeGreaterThan(0);
  });

  it('shows password form only for local users', async () => {
    apiClient.get.mockResolvedValueOnce({ data: LOCAL_PROFILE });
    renderProfile();

    await waitFor(() => expect(screen.getByText(/change password/i)).toBeInTheDocument());
    expect(screen.getByLabelText(/current password/i)).toBeInTheDocument();
  });

  it('hides password form for oauth2 users', async () => {
    apiClient.get.mockResolvedValueOnce({ data: OAUTH_PROFILE });
    renderProfile();

    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument());
    expect(screen.queryByLabelText(/current password/i)).not.toBeInTheDocument();
    expect(screen.getByText(/password management is not available/i)).toBeInTheDocument();
  });

  it('shows error state when API fails', async () => {
    apiClient.get.mockRejectedValueOnce(new Error('Network error'));
    renderProfile();

    await waitFor(() => expect(screen.getByText(/failed to load profile/i)).toBeInTheDocument());
  });

  it('shows validation error when new passwords do not match', async () => {
    apiClient.get.mockResolvedValueOnce({ data: LOCAL_PROFILE });
    renderProfile();

    await waitFor(() => expect(screen.getByLabelText(/current password/i)).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'oldPass1' } });
    fireEvent.change(screen.getByLabelText(/^new password/i), { target: { value: 'newPass123' } });
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'different' } });
    fireEvent.click(screen.getByRole('button', { name: /update password/i }));

    await waitFor(() => expect(screen.getByText(/do not match/i)).toBeInTheDocument());
    expect(apiClient.patch).not.toHaveBeenCalled();
  });

  it('shows success message after password change', async () => {
    apiClient.get.mockResolvedValueOnce({ data: LOCAL_PROFILE });
    apiClient.patch.mockResolvedValueOnce({});
    renderProfile();

    await waitFor(() => expect(screen.getByLabelText(/current password/i)).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'oldPass1' } });
    fireEvent.change(screen.getByLabelText(/^new password/i), { target: { value: 'newPass123' } });
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'newPass123' } });
    fireEvent.click(screen.getByRole('button', { name: /update password/i }));

    await waitFor(() => expect(screen.getByText(/password updated successfully/i)).toBeInTheDocument());
  });

  it('shows API error message on failed password change', async () => {
    apiClient.get.mockResolvedValueOnce({ data: LOCAL_PROFILE });
    apiClient.patch.mockRejectedValueOnce({
      response: { data: { detail: 'Current password is incorrect' } },
    });
    renderProfile();

    await waitFor(() => expect(screen.getByLabelText(/current password/i)).toBeInTheDocument());

    fireEvent.change(screen.getByLabelText(/current password/i), { target: { value: 'wrong' } });
    fireEvent.change(screen.getByLabelText(/^new password/i), { target: { value: 'newPass123' } });
    fireEvent.change(screen.getByLabelText(/confirm new password/i), { target: { value: 'newPass123' } });
    fireEvent.click(screen.getByRole('button', { name: /update password/i }));

    await waitFor(() => expect(screen.getByText(/current password is incorrect/i)).toBeInTheDocument());
  });

  it('shows last login date', async () => {
    apiClient.get.mockResolvedValueOnce({ data: LOCAL_PROFILE });
    renderProfile();

    await waitFor(() => expect(screen.getByText(/last login/i)).toBeInTheDocument());
  });

  it('shows dash when last login is null', async () => {
    apiClient.get.mockResolvedValueOnce({ data: { ...LOCAL_PROFILE, lastLogin: null } });
    renderProfile();

    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument());
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
  });

  it('back button navigates to dashboard', async () => {
    apiClient.get.mockResolvedValueOnce({ data: LOCAL_PROFILE });
    renderProfile();

    await waitFor(() => expect(screen.getByText('alice')).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /back to dashboard/i }));

    await waitFor(() => expect(screen.getByText('Dashboard')).toBeInTheDocument());
  });
});
