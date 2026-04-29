import { describe, it, expect, beforeEach } from 'vitest';
import { authService } from '../services/authService';

const createValidJWT = (expiresIn = 3600) => {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const now = Math.floor(Date.now() / 1000);
  const payload = btoa(JSON.stringify({ sub: 'test', exp: now + expiresIn }));
  const signature = 'test-sig';
  return `${header}.${payload}.${signature}`;
};

const createExpiredJWT = () => {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const now = Math.floor(Date.now() / 1000);
  const payload = btoa(JSON.stringify({ sub: 'test', exp: now - 3600 }));
  const signature = 'test-sig';
  return `${header}.${payload}.${signature}`;
};

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
    authService.saveToken(createValidJWT());
    expect(authService.isAuthenticated()).toBe(true);
  });

  it('isAuthenticated returns false when token expired', () => {
    authService.saveToken(createExpiredJWT());
    expect(authService.isAuthenticated()).toBe(false);
  });

  it('logout clears both tokens', () => {
    authService.saveToken(createValidJWT());
    authService.saveRefreshToken('rt');
    authService.logout();
    expect(authService.getToken()).toBeNull();
    expect(authService.getRefreshToken()).toBeNull();
    expect(authService.isAuthenticated()).toBe(false);
  });
});
