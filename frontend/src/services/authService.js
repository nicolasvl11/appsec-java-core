import apiClient from './apiClient';

const decodeToken = (token) => {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const decoded = JSON.parse(atob(parts[1]));
    return decoded;
  } catch {
    return null;
  }
};

const isTokenExpired = (token) => {
  const decoded = decodeToken(token);
  if (!decoded || !decoded.exp) return true;
  return decoded.exp * 1000 < Date.now();
};

export const authService = {
  login(username, password) {
    return apiClient.post('/api/v1/auth/login', { username, password });
  },

  register(username, password) {
    return apiClient.post('/api/v1/auth/register', { username, password });
  },

  saveToken(token) {
    localStorage.setItem('jwt', token);
  },

  saveRefreshToken(token) {
    localStorage.setItem('refreshToken', token);
  },

  getToken() {
    return localStorage.getItem('jwt');
  },

  getRefreshToken() {
    return localStorage.getItem('refreshToken');
  },

  getUsername() {
    const token = this.getToken();
    if (!token) return null;
    return decodeToken(token)?.sub ?? null;
  },

  getRole() {
    const token = this.getToken();
    if (!token) return null;
    return decodeToken(token)?.role ?? null;
  },

  isAdmin() {
    return this.getRole() === 'ADMIN';
  },

  logout() {
    localStorage.removeItem('jwt');
    localStorage.removeItem('refreshToken');
  },

  isAuthenticated() {
    const token = localStorage.getItem('jwt');
    return Boolean(token) && !isTokenExpired(token);
  },
};
