// Environment configuration
const config = {
  development: {
    API_BASE_URL: 'http://localhost:8080/api',
    SHORT_URL_BASE: 'localhost:8080/api',
    AUTH_BASE_URL: 'http://localhost:8080',
    OAUTH_BASE_URL: 'http://localhost:8080/oauth2/authorization'
  },
  production: {
    API_BASE_URL: 'https://zaplink-api-dot-rice-comp-539-spring-2022.uk.r.appspot.com/api',
    SHORT_URL_BASE: 'zaplink-api-dot-rice-comp-539-spring-2022.uk.r.appspot.com/api',
    AUTH_BASE_URL: 'https://zaplink-api-dot-rice-comp-539-spring-2022.uk.r.appspot.com',
    OAUTH_BASE_URL: 'https://zaplink-api-dot-rice-comp-539-spring-2022.uk.r.appspot.com/oauth2/authorization'
  }
};

// Get current environment
const ENV = process.env.NODE_ENV || 'development';

// Export configuration based on environment
export const API_BASE_URL = config[ENV].API_BASE_URL;
export const SHORT_URL_BASE = config[ENV].SHORT_URL_BASE;
export const AUTH_BASE_URL = config[ENV].AUTH_BASE_URL;
export const OAUTH_BASE_URL = config[ENV].OAUTH_BASE_URL; 