// services/authService.js
import apiClient from './apiClient';

export const login = async (username, password) => {
  const response = await apiClient.post('/auth/login', { username, password });
  if (response.data.token) {
    localStorage.setItem('authToken', response.data.token);
    localStorage.setItem('userId', response.data.userId);
    localStorage.setItem('username', response.data.username);
  }
  return response.data;
};

export const register = async (username, email, password) => {
  return await apiClient.post('/users/register', { username, email, password });
};

export const logout = () => {
  localStorage.removeItem('authToken');
  localStorage.removeItem('userId');
  localStorage.removeItem('username');
};

export const getCurrentUser = () => {
  return {
    token: localStorage.getItem('authToken'),
    userId: localStorage.getItem('userId'),
    username: localStorage.getItem('username')
  };
};