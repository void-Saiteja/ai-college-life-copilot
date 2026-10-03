import axios from 'axios';

const rawUrl = import.meta.env.VITE_API_URL;

let API_BASE_URL;

if (rawUrl && rawUrl.trim()) {
  const cleanUrl = rawUrl.trim().replace(/\/+$/, '');
  API_BASE_URL = cleanUrl.endsWith('/api')
    ? cleanUrl
    : `${cleanUrl}/api`;
} else {
  API_BASE_URL = import.meta.env.PROD
    ? '/api'
    : 'http://localhost:5000/api';
}

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Attach JWT token to requests
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('copilot_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export default api;
