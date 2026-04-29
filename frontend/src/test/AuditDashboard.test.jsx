import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import AuditDashboard from '../components/AuditDashboard';

vi.mock('../services/apiClient', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}));

import apiClient from '../services/apiClient';

const PAGE = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };

function renderDashboard() {
  return render(
    <MemoryRouter initialEntries={['/dashboard']}>
      <Routes>
        <Route path="/dashboard" element={<AuditDashboard />} />
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('AuditDashboard', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('shows loading then empty state', async () => {
    apiClient.get.mockResolvedValueOnce({ data: PAGE });
    renderDashboard();

    const skeletonLoaders = document.querySelectorAll('.animate-pulse');
    expect(skeletonLoaders.length).toBeGreaterThan(0);
    await waitFor(() => expect(screen.getAllByText(/no audit events found/i).length).toBeGreaterThan(0));
  });

  it('renders event rows from API response', async () => {
    const event = {
      id: 1,
      eventTime: '2024-01-01T00:00:00Z',
      actor: 'alice',
      action: 'http_request',
      target: '/api/v1/ping',
    };
    apiClient.get.mockResolvedValueOnce({
      data: { ...PAGE, content: [event], totalElements: 1, totalPages: 1 },
    });

    renderDashboard();

    await waitFor(() => expect(screen.getAllByText('alice').length).toBeGreaterThan(0), { timeout: 3000 });
    expect(screen.getAllByText('http_request').length).toBeGreaterThan(0);
    expect(screen.getAllByText('/api/v1/ping').length).toBeGreaterThan(0);
  });

  it('shows error message when API fails', async () => {
    apiClient.get.mockRejectedValueOnce(new Error('Network error'));
    renderDashboard();

    await waitFor(() =>
      expect(screen.getByText(/failed to load/i)).toBeInTheDocument()
    );
  });

  it('shows pagination when totalPages > 1', async () => {
    apiClient.get.mockResolvedValue({
      data: { content: [], page: 0, size: 20, totalElements: 50, totalPages: 3 },
    });

    renderDashboard();

    await waitFor(() => expect(screen.getByText(/page 1 of 3/i)).toBeInTheDocument());
    expect(screen.getByRole('button', { name: /next/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /previous/i })).toBeDisabled();
  });

  it('logout button calls API and redirects', async () => {
    apiClient.get.mockResolvedValueOnce({ data: PAGE });
    apiClient.post.mockResolvedValueOnce({});

    renderDashboard();
    await waitFor(() => screen.getByRole('button', { name: /logout/i }));

    fireEvent.click(screen.getByRole('button', { name: /logout/i }));

    await waitFor(() => expect(screen.getByText('Login page')).toBeInTheDocument());
  });

  it('filters events by actor', async () => {
    const events = [
      { id: 1, eventTime: '2024-01-01T00:00:00Z', actor: 'alice', action: 'login', target: '/auth' },
      { id: 2, eventTime: '2024-01-02T00:00:00Z', actor: 'bob', action: 'logout', target: '/auth' },
    ];
    apiClient.get.mockResolvedValue({
      data: { content: [events[0]], page: 0, size: 20, totalElements: 1, totalPages: 1 },
    });

    renderDashboard();
    const actorInput = screen.getByPlaceholderText('Filter by actor...');
    fireEvent.change(actorInput, { target: { value: 'alice' } });

    await waitFor(() => {
      expect(apiClient.get).toHaveBeenCalledWith(expect.stringContaining('actor=alice'));
    });
  });

  it('filters events by action', async () => {
    apiClient.get.mockResolvedValue({ data: PAGE });

    renderDashboard();
    const actionInput = screen.getByPlaceholderText('Filter by action...');
    fireEvent.change(actionInput, { target: { value: 'login' } });

    await waitFor(() => {
      expect(apiClient.get).toHaveBeenCalledWith(expect.stringContaining('action=login'));
    });
  });

  it('clears all filters on clear button click', async () => {
    apiClient.get.mockResolvedValue({ data: PAGE });

    renderDashboard();
    const actorInput = screen.getByPlaceholderText('Filter by actor...');
    fireEvent.change(actorInput, { target: { value: 'alice' } });

    await waitFor(() => expect(screen.getByText(/filter.*active/i)).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /clear/i }));

    expect(actorInput.value).toBe('');
    expect(apiClient.get).toHaveBeenCalledWith(expect.stringContaining('?page=0&size=20'));
  });
});
