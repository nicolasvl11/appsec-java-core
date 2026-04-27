import apiClient from './apiClient';

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

  logout() {
    localStorage.removeItem('jwt');
    localStorage.removeItem('refreshToken');
  },

  isAuthenticated() {
    return Boolean(localStorage.getItem('jwt'));
  },
};
