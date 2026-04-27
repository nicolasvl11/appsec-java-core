import apiClient from './apiClient';

export const authService = {
  login(username, password) {
    return apiClient.post('/api/v1/auth/login', { username, password });
  },

  register(username, password) {
    return apiClient.post('/api/v1/auth/register', { username, password });
  },

  exchangeOAuthCode(code) {
    return apiClient.post(`/api/v1/auth/callback?code=${encodeURIComponent(code)}`);
  },

  saveToken(token) {
    localStorage.setItem('jwt', token);
  },

  getToken() {
    return localStorage.getItem('jwt');
  },

  logout() {
    localStorage.removeItem('jwt');
  },

  isAuthenticated() {
    return Boolean(localStorage.getItem('jwt'));
  },
};
