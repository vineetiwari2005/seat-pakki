import axios from 'axios';
import { API_BASE_URL, buildApiUrl } from '../config/apiBaseUrl';

// Create axios instance with base configuration
const api = axios.create({
  // In production, use VITE_API_BASE_URL; in dev, keep empty to use Vite proxy.
  baseURL: API_BASE_URL || '',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add JWT token and theatre override header
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    // Send selected theatre ID for /owner routes (theatre admin city/theatre selection)
    if (config.url && config.url.startsWith('/owner')) {
      const theatreId = localStorage.getItem('selectedTheatreId');
      if (theatreId) {
        config.headers['X-Theatre-Id'] = theatreId;
      }
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        const response = await axios.post(buildApiUrl('/auth/refresh'), {
          refreshToken,
        });
        
        const { accessToken } = response.data;
        localStorage.setItem('accessToken', accessToken);
        
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
