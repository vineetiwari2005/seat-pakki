const rawBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').trim();
const productionFallbackBaseUrl = 'https://seat-pakki-backend-latest-5.onrender.com';

const resolvedBaseUrl = rawBaseUrl || (import.meta.env.PROD ? productionFallbackBaseUrl : '');

export const API_BASE_URL = resolvedBaseUrl.replace(/\/$/, '');

export const buildApiUrl = (path = '') => {
  if (!path) {
    return API_BASE_URL || '';
  }

  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return API_BASE_URL ? `${API_BASE_URL}${normalizedPath}` : normalizedPath;
};
