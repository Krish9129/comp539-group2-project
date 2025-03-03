import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

// Create axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Add auth token to all requests
apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// URL shortening service
const urlService = {
  // Create short URL
  shortenUrl: async (originalUrl, alias = '', tag = '') => {
    try {
      // Create query parameters in URL format instead of form data
      let url = `/shorten?url=${encodeURIComponent(originalUrl)}`;
      
      if (alias) {
        url += `&alias=${encodeURIComponent(alias)}`;
      }
      
      if (tag) {
        url += `&tag=${encodeURIComponent(tag)}`;
      }
      
      const response = await apiClient.post(url);
      return response.data;
    } catch (error) {
      console.error('Error shortening URL:', error);
      throw error;
    }
  },
  
  // Bulk create short URLs
  bulkShortenUrls: async (urls) => {
    try {
      const response = await apiClient.post('/bulk-shorten', urls);
      return response.data;
    } catch (error) {
      console.error('Error bulk shortening URLs:', error);
      throw error;
    }
  },
  
  // Get URLs by tag
  getUrlsByTag: async (tag) => {
    try {
      // Ensure tag is properly encoded
      const encodedTag = encodeURIComponent(tag);
      const response = await apiClient.get(`/urls?tag=${encodedTag}`);
      return response.data;
    } catch (error) {
      console.error('Error getting URLs by tag:', error);
      throw error;
    }
  },
  
  // Delete short URL
  deleteUrl: async (id) => {
    try {
      const response = await apiClient.delete(`/${id}`);
      return response.data;
    } catch (error) {
      console.error('Error deleting URL:', error);
      throw error;
    }
  },
  
  // Get QR code URL
  getQrCodeUrl: (shortId) => {
    return `${API_BASE_URL}/${shortId}/qr`;
  }
};

export default urlService;