import React, { createContext, useState, useEffect, useContext } from 'react';
import authService from '../services/authService';

// Create context
const AuthContext = createContext();

// Create provider
export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  // Load user on mount
  useEffect(() => {
    let isMounted = true;
    
    const loadUser = async () => {
      if (authService.isAuthenticated()) {
        try {
          const userData = await authService.getUserProfile();
          if (isMounted) { 
            setUser(userData);
            setIsAuthenticated(true);
          }
        } catch (error) {
          console.error('Error loading user:', error);
          if (isMounted && error.response && error.response.status === 401) {
            authService.logout();
            setUser(null);
            setIsAuthenticated(false);
          }
        }
      } else {
        if (isMounted) {
          setUser(null);
          setIsAuthenticated(false);
        }
      }
      if (isMounted) {
        setLoading(false);
      }
    };
  
    loadUser();
    
    return () => { isMounted = false; }; 
  }, []);

  // Enhanced login function that can either:
  // 1. Start OAuth flow when no token is provided
  // 2. Set authentication state directly when token is provided
  const login = (providerOrToken) => {
    // If providerOrToken is an OAuth token, not a provider name
    if (typeof providerOrToken === 'string' && providerOrToken.length > 20) {
      // This is likely a token from OAuth callback
      console.log('Setting authentication with token');
      setIsAuthenticated(true);
      // Load user data with the new token
      loadUserWithToken(providerOrToken);
      return;
    }
    
    // Normal OAuth flow - redirect to auth provider
    authService.login(providerOrToken);
  };
  
  // Load user data with a specific token
  const loadUserWithToken = async (token) => {
    try {
      console.log('Loading user profile with token');
      const userData = await authService.getUserProfile();
      console.log('User profile loaded:', userData);
      setUser(userData);
    } catch (error) {
      console.error('Error loading user with token:', error);
    }
  };

  // Logout function
  const logout = () => {
    authService.logout();
    setUser(null);
    setIsAuthenticated(false);
  };
  
  // Debug function to help troubleshoot
  const debugAuth = () => {
    console.log('Auth Status:', isAuthenticated);
    console.log('Token:', localStorage.getItem('auth_token'));
    console.log('User:', user);
    
    // Check if token exists in localStorage
    if (localStorage.getItem('auth_token')) {
      const token = localStorage.getItem('auth_token');
      console.log('Token exists:', token.substring(0, 15) + '...');
      
      // Decode JWT token parts (for debugging only)
      try {
        const parts = token.split('.');
        if (parts.length === 3) {
          const header = JSON.parse(atob(parts[0]));
          const payload = JSON.parse(atob(parts[1]));
          console.log('Token header:', header);
          console.log('Token payload:', payload);
          console.log('Token expiration:', new Date(payload.exp * 1000).toLocaleString());
        }
      } catch (e) {
        console.error('Error decoding token:', e);
      }
    } else {
      console.log('No token found in localStorage');
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        isAuthenticated,
        login,
        logout,
        debugAuth
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

// Create hook for using the context
export const useAuth = () => useContext(AuthContext);

export default AuthContext;