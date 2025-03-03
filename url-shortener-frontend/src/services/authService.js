import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

// Create axios instance for auth
const authClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Add auth token to all requests that need it
authClient.interceptors.request.use(
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


let profileCache = null;
let lastFetchTime = 0;
const CACHE_DURATION = 10000; // 10 seconds
let pendingRequest = null;

const authService = {
  // Initiate OAuth login
  login: (provider) => {
    // Redirect to the backend authentication endpoint with full URL
    const redirectUrl = `http://localhost:8080/oauth2/authorization/${provider}`;
    window.location.href = redirectUrl;
  },
  
  // Process OAuth callback and save token
  handleCallback: () => {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    
    if (token) {
      // Save token to localStorage
      localStorage.setItem('auth_token', token);

      profileCache = null;
      lastFetchTime = 0;
      pendingRequest = null;
      return true;
    }
    return false;
  },
  
  // Check if user is logged in
  isAuthenticated: () => {
    return !!localStorage.getItem('auth_token');
  },
  
  // Log out
  logout: () => {
    localStorage.removeItem('auth_token');
    // Clear profile cache
    profileCache = null;
    lastFetchTime = 0;
    pendingRequest = null;
    window.location.href = '/';
  },
  
  // Get user profile
  getUserProfile: async () => {
    const currentTime = Date.now();
    
    // if cache exists and not expired, return cached data
    if (profileCache && (currentTime - lastFetchTime < CACHE_DURATION)) {
      console.log('Returning cached profile data');
      return Promise.resolve(profileCache);
    }
    
    // if there is a pending request, return it
    if (pendingRequest) {
      console.log('Returning pending profile request');
      return pendingRequest;
    }
    
    try {
      console.log('Making new profile request with token:', localStorage.getItem('auth_token').substring(0, 20) + '...');
      
      // initiate new request
      pendingRequest = authClient.get('/api/user/profile').then(response => {
        // update cache and fetch time
        profileCache = response.data;
        lastFetchTime = Date.now();
        console.log('Profile data received:', response.data.id);
        
        // reset pendingRequest
        pendingRequest = null;
        return response.data;
      }).catch(error => {
        pendingRequest = null;
        console.error('Error getting user profile:', error);
        throw error;
      });
      
      return pendingRequest;
    } catch (error) {
      pendingRequest = null;
      console.error('Error initiating profile request:', error);
      throw error;
    }
  },
  
  // Clear profile cache
  clearProfileCache: () => {
    profileCache = null;
    lastFetchTime = 0;
    pendingRequest = null;
  },
  
  // Get JWT token
  getToken: () => {
    return localStorage.getItem('auth_token');
  }
};

export default authService;