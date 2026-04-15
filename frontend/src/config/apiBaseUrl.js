const rawBaseUrl = (import.meta.env.VITE_API_BASE_URL || '').trim();

export const API_BASE_URL = rawBaseUrl.replace(/\/$/, '');

export const buildApiUrl = (path = '') => {
  if (!path) {
    return API_BASE_URL || '';
  }

  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return API_BASE_URL ? `${API_BASE_URL}${normalizedPath}` : normalizedPath;
};
