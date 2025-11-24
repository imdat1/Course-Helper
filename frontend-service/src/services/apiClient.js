import axios from 'axios';

// Use the proxy configuration from package.json instead of hardcoding the URL
const apiClient = axios.create({
  baseURL: '/api',  // This will be proxied to http://localhost:8080/api
  headers: {
    'Content-Type': 'application/json',
  },
});

const token = localStorage.getItem('authToken');
if (token) {
  console.log('Current token (first 20 chars):', token.substring(0, 20) + '...');
  
  // Check if it's a proper JWT format (should have two dots)
  if (token.split('.').length !== 3) {
    console.warn('Token doesn\'t appear to be in JWT format!');
  }
} else {
  console.warn('No auth token found in localStorage!');
}

// Request interceptor to add auth token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    
    console.log('Request URL:', config.url);
    console.log('Request method:', config.method);
    
    if (token) {
      // Make sure to use the exact format your backend expects
      console.log('Auth token found, adding to request headers');
      config.headers.Authorization = `Bearer ${token}`;
      
      // Log the full headers for debugging
      console.log('Request headers:', config.headers);
    } else {
      console.warn('No auth token found in localStorage!');
    }
    
    return config;
  },
  (error) => {
    console.error('Request interceptor error:', error);
    return Promise.reject(error);
  }
);

// Request interceptor with verbose logging
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    
    console.log('Request URL:', config.url);
    console.log('Request method:', config.method);
    
    if (token) {
      console.log('Auth token found, adding to request headers');
      config.headers.Authorization = `Bearer ${token}`;
    } else {
      console.warn('No auth token found in localStorage!');
    }
    
    return config;
  },
  (error) => {
    console.error('Request interceptor error:', error);
    return Promise.reject(error);
  }
);

// Response interceptor for debugging
apiClient.interceptors.response.use(
  (response) => {
    console.log('Response from:', response.config.url);
    console.log('Status:', response.status);
    return response;
  },
  (error) => {
    console.error('API Error:', error);
    if (error.response) {
      console.error('Error status:', error.response.status);
      console.error('Error data:', error.response.data);
    }
    return Promise.reject(error);
  }
);

export default apiClient;