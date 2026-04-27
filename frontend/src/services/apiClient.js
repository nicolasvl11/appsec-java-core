import axios from 'axios';
import { authService } from './authService';

const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = authService.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const isAuthEndpoint = original.url?.includes('/api/v1/auth/');

    // For auth endpoints or already-retried requests, fail immediately
    if (error.response?.status !== 401 || isAuthEndpoint || original._retried) {
      if (error.response?.status === 401 && !isAuthEndpoint) {
        authService.logout();
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }

    original._retried = true;
    const refreshToken = authService.getRefreshToken();

    if (!refreshToken) {
      authService.logout();
      window.location.href = '/login';
      return Promise.reject(error);
    }

    try {
      const res = await apiClient.post('/api/v1/auth/refresh', { refreshToken });
      authService.saveToken(res.data.token);
      authService.saveRefreshToken(res.data.refreshToken);
      original.headers['Authorization'] = `Bearer ${res.data.token}`;
      return apiClient(original);
    } catch {
      authService.logout();
      window.location.href = '/login';
      return Promise.reject(error);
    }
  }
);

export default apiClient;
