import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import AdminPage from '../components/AdminPage';

vi.mock('../services/apiClient', () => ({
  default: {
    get: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}));

vi.mock('../services/authService', () => ({
  authService: {
    getUsername: vi.fn(() => 'admin'),
    isAdmin: vi.fn(() => true),
    isAuthenticated: vi.fn(() => true),
    getToken: vi.fn(() => 'tok'),
    getRole: vi.fn(() => 'ADMIN'),
  },
}));

import apiClient from '../services/apiClient';

const PAGE_BASE = { page: 0, size: 20, totalElements: 0, totalPages: 0, content: [] };

function user(id, username, role = 'USER', enabled = true) {
  return { id, username, role, enabled, createdAt: '2024-01-01T00:00:00Z', provider: null, lastLogin: null };
}

function renderAdmin() {
  return render(
    <MemoryRouter initialEntries={['/admin']}>
      <Routes>
        <Route path="/admin" element={<AdminPage />} />
        <Route path="/dashboard" element={<div>Dashboard</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('AdminPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows skeleton while loading', () => {
    apiClient.get.mockReturnValueOnce(new Promise(() => {}));
    renderAdmin();
    expect(document.querySelectorAll('.animate-pulse').length).toBeGreaterThan(0);
  });

  it('renders user table after load', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(1, 'bob')], totalElements: 1 },
    });
    renderAdmin();

    await waitFor(() => expect(screen.getAllByText('bob').length).toBeGreaterThan(0));
    expect(screen.getAllByText('USER').length).toBeGreaterThan(0);
  });

  it('shows total user count', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(1, 'bob')], totalElements: 1 },
    });
    renderAdmin();

    await waitFor(() => expect(screen.getByText(/1 registered user/i)).toBeInTheDocument());
  });

  it('shows empty state when no users', async () => {
    apiClient.get.mockResolvedValueOnce({ data: PAGE_BASE });
    renderAdmin();

    await waitFor(() => expect(screen.getAllByText(/no users found/i).length).toBeGreaterThan(0));
  });

  it('shows error state when API fails', async () => {
    apiClient.get.mockRejectedValueOnce(new Error('Network error'));
    renderAdmin();

    await waitFor(() => expect(screen.getByText(/failed to load users/i)).toBeInTheDocument());
  });

  it('retry button reloads users', async () => {
    apiClient.get
      .mockRejectedValueOnce(new Error('fail'))
      .mockResolvedValueOnce({ data: { ...PAGE_BASE, content: [user(1, 'bob')], totalElements: 1 } });

    renderAdmin();

    await waitFor(() => expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /retry/i }));

    await waitFor(() => expect(screen.getAllByText('bob').length).toBeGreaterThan(0));
  });

  it('shows dash for self — cannot change own role', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(1, 'admin', 'ADMIN')], totalElements: 1 },
    });
    renderAdmin();

    await waitFor(() => expect(screen.getAllByText('admin').length).toBeGreaterThan(0));
    expect(screen.queryByRole('button', { name: /make/i })).not.toBeInTheDocument();
    expect(screen.getAllByText('—').length).toBeGreaterThan(0);
  });

  it('shows Make Admin button for USER', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(2, 'bob', 'USER')], totalElements: 1 },
    });
    renderAdmin();

    await waitFor(() => expect(screen.getAllByRole('button', { name: /make admin/i }).length).toBeGreaterThan(0));
  });

  it('shows Make User button for ADMIN', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(2, 'bob', 'ADMIN')], totalElements: 1 },
    });
    renderAdmin();

    await waitFor(() => expect(screen.getAllByRole('button', { name: /make user/i }).length).toBeGreaterThan(0));
  });

  it('toggles role on button click', async () => {
    const updated = { ...user(2, 'bob', 'ADMIN'), createdAt: '2024-01-01T00:00:00Z' };
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(2, 'bob', 'USER')], totalElements: 1 },
    });
    apiClient.patch.mockResolvedValueOnce({ data: updated });

    renderAdmin();

    await waitFor(() => expect(screen.getAllByRole('button', { name: /make admin/i }).length).toBeGreaterThan(0));
    fireEvent.click(screen.getAllByRole('button', { name: /make admin/i })[0]);

    await waitFor(() => expect(screen.getAllByRole('button', { name: /make user/i }).length).toBeGreaterThan(0));
    expect(apiClient.patch).toHaveBeenCalledWith('/api/v1/admin/users/2/role', { role: 'ADMIN' });
  });

  it('back button navigates to dashboard', async () => {
    apiClient.get.mockResolvedValueOnce({ data: PAGE_BASE });
    renderAdmin();

    await waitFor(() => expect(screen.getAllByText(/no users found/i).length).toBeGreaterThan(0));
    fireEvent.click(screen.getByRole('button', { name: /back to dashboard/i }));

    await waitFor(() => expect(screen.getByText('Dashboard')).toBeInTheDocument());
  });

  it('hides pagination when only one page', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(1, 'bob')], totalElements: 1, totalPages: 1 },
    });
    renderAdmin();

    await waitFor(() => expect(screen.getAllByText('bob').length).toBeGreaterThan(0));
    expect(screen.queryByRole('button', { name: /previous/i })).not.toBeInTheDocument();
  });

  it('toggles enabled status on Disable click', async () => {
    const disabled = { ...user(2, 'bob', 'USER', true), createdAt: '2024-01-01T00:00:00Z' };
    disabled.enabled = false;
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(2, 'bob', 'USER', true)], totalElements: 1 },
    });
    apiClient.patch.mockResolvedValueOnce({ data: disabled });

    renderAdmin();

    await waitFor(() => expect(screen.getAllByRole('button', { name: /disable/i }).length).toBeGreaterThan(0));
    fireEvent.click(screen.getAllByRole('button', { name: /disable/i })[0]);

    await waitFor(() => expect(screen.getAllByRole('button', { name: /enable/i }).length).toBeGreaterThan(0));
    expect(apiClient.patch).toHaveBeenCalledWith('/api/v1/admin/users/2/status', { enabled: false });
  });

  it('shows Disable button for active user', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(2, 'bob', 'USER', true)], totalElements: 1 },
    });
    renderAdmin();

    await waitFor(() => expect(screen.getAllByRole('button', { name: /disable/i }).length).toBeGreaterThan(0));
  });

  it('shows Enable button for disabled user', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(2, 'bob', 'USER', false)], totalElements: 1 },
    });
    renderAdmin();

    await waitFor(() => expect(screen.getAllByRole('button', { name: /enable/i }).length).toBeGreaterThan(0));
  });

  it('shows Delete button for other users', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(2, 'bob')], totalElements: 1 },
    });
    renderAdmin();

    await waitFor(() => expect(screen.getAllByRole('button', { name: /delete bob/i }).length).toBeGreaterThan(0));
  });

  it('shows confirm modal before deleting', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(2, 'bob')], totalElements: 1 },
    });
    renderAdmin();

    await waitFor(() => expect(screen.getAllByRole('button', { name: /delete bob/i }).length).toBeGreaterThan(0));
    fireEvent.click(screen.getAllByRole('button', { name: /delete bob/i })[0]);

    await waitFor(() => expect(screen.getByText(/permanently delete/i)).toBeInTheDocument());
    expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
  });

  it('removes user from list after confirmed delete', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(2, 'bob')], totalElements: 1 },
    });
    apiClient.delete.mockResolvedValueOnce({});
    renderAdmin();

    await waitFor(() => expect(screen.getAllByText('bob').length).toBeGreaterThan(0));
    fireEvent.click(screen.getAllByRole('button', { name: /delete bob/i })[0]);
    await waitFor(() => expect(screen.getByRole('button', { name: /^delete$/i })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /^delete$/i }));

    await waitFor(() => expect(screen.queryByText('bob')).not.toBeInTheDocument());
    expect(apiClient.delete).toHaveBeenCalledWith('/api/v1/admin/users/2');
  });

  it('cancel on confirm modal does not delete', async () => {
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE_BASE, content: [user(2, 'bob')], totalElements: 1 },
    });
    renderAdmin();

    await waitFor(() => expect(screen.getAllByRole('button', { name: /delete bob/i }).length).toBeGreaterThan(0));
    fireEvent.click(screen.getAllByRole('button', { name: /delete bob/i })[0]);
    await waitFor(() => expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /cancel/i }));

    await waitFor(() => expect(screen.queryByText(/permanently delete/i)).not.toBeInTheDocument());
    expect(apiClient.delete).not.toHaveBeenCalled();
    expect(screen.getAllByText('bob').length).toBeGreaterThan(0);
  });
});
