import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../services/authService';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    try {
      const res = await authService.login(username, password);
      authService.saveToken(res.data.token);
      authService.saveRefreshToken(res.data.refreshToken);
      navigate('/dashboard');
    } catch (err) {
      const status = err.response?.status;
      if (status === 423) {
        const secs = err.response?.data?.retryAfterSeconds ?? 900;
        setError(`Account locked. Try again in ${Math.ceil(secs / 60)} minutes.`);
      } else {
        setError('Invalid credentials');
      }
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="bg-white shadow rounded-lg p-8 w-full max-w-md space-y-6">
        <h1 className="text-2xl font-bold text-center text-gray-800">AppSec Java Core</h1>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="username" className="block text-sm font-medium text-gray-700">Username</label>
            <input
              id="username"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              className="mt-1 block w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="mt-1 block w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          {error && <p className="text-red-600 text-sm">{error}</p>}
          <button
            type="submit"
            className="w-full bg-indigo-600 text-white py-2 rounded hover:bg-indigo-700 transition"
          >
            Sign In
          </button>
        </form>

        <div className="relative">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-gray-200" />
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="px-2 bg-white text-gray-500">or continue with</span>
          </div>
        </div>

        <div className="space-y-3">
          <a
            href="http://localhost:8080/oauth2/authorization/google"
            className="flex items-center justify-center w-full border border-gray-300 rounded py-2 hover:bg-gray-50 transition text-sm font-medium text-gray-700"
          >
            Sign in with Google
          </a>
          <a
            href="http://localhost:8080/oauth2/authorization/github"
            className="flex items-center justify-center w-full border border-gray-300 rounded py-2 hover:bg-gray-50 transition text-sm font-medium text-gray-700"
          >
            Sign in with GitHub
          </a>
        </div>

        <p className="text-center text-sm text-gray-500">
          No account?{' '}
          <Link to="/register" className="text-indigo-600 hover:underline">
            Register
          </Link>
        </p>
      </div>
    </div>
  );
}
