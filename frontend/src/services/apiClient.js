import axios from 'axios';
import { authService } from './authService';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});

apiClient.interceptors.request.use((config) => {
  const token = authService.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshPromise = null;

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    const isAuthEndpoint = original.url?.includes('/api/v1/auth/');

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

    if (!refreshPromise) {
      refreshPromise = apiClient
        .post('/api/v1/auth/refresh', { refreshToken })
        .then((res) => {
          authService.saveToken(res.data.token);
          authService.saveRefreshToken(res.data.refreshToken);
          return res.data.token;
        })
        .catch((err) => {
          authService.logout();
          window.location.href = '/login';
          throw err;
        })
        .finally(() => {
          refreshPromise = null;
        });
    }

    return refreshPromise.then((token) => {
      original.headers['Authorization'] = `Bearer ${token}`;
      return apiClient(original);
    });
  }
);

export default apiClient;
