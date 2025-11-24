// services/chatService.js
import apiClient from './apiClient';

export const getMessages = async (courseId) => {
  return await apiClient.get(`/courses/${courseId}/messages`);
};

export const sendMessage = async (courseId, content) => {
  const message = {
    role: 'user',
    content: content
  };
  return await apiClient.post(`/courses/${courseId}/chat`, message);
};