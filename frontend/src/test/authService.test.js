import { describe, it, expect, beforeEach } from 'vitest';
import { authService } from '../services/authService';

// authService uses apiClient which makes HTTP calls — test localStorage logic only
describe('authService', () => {
  beforeEach(() => localStorage.clear());

  it('saves and retrieves access token', () => {
    authService.saveToken('abc');
    expect(authService.getToken()).toBe('abc');
  });

  it('saves and retrieves refresh token', () => {
    authService.saveRefreshToken('rt-abc');
    expect(authService.getRefreshToken()).toBe('rt-abc');
  });

  it('isAuthenticated returns false when no token', () => {
    expect(authService.isAuthenticated()).toBe(false);
  });

  it('isAuthenticated returns true when token present', () => {
    authService.saveToken('tok');
    expect(authService.isAuthenticated()).toBe(true);
  });

  it('logout clears both tokens', () => {
    authService.saveToken('tok');
    authService.saveRefreshToken('rt');
    authService.logout();
    expect(authService.getToken()).toBeNull();
    expect(authService.getRefreshToken()).toBeNull();
    expect(authService.isAuthenticated()).toBe(false);
  });
});
