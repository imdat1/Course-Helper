// services/courseService.js
import apiClient from './apiClient';

export const getCourses = async () => {
  const userId = localStorage.getItem('userId');
  if (!userId) {
    console.error('No user ID found in localStorage');
    throw new Error('User ID not found. Please log in again.');
  }

  console.log('Fetching courses for user ID:', userId);
  
  try {
    // First try the user-specific endpoint
    const response = await apiClient.get(`/courses/user/${userId}`);
    console.log('Courses response:', response);
    
    // If that didn't work, try the generic courses endpoint
    if (!response.data || (Array.isArray(response.data) && response.data.length === 0)) {
      console.log('No courses found with user endpoint, trying general endpoint');
      const allCoursesResponse = await apiClient.get('/courses');
      console.log('All courses response:', allCoursesResponse);
      
      // Filter courses by owner ID
      if (allCoursesResponse.data && Array.isArray(allCoursesResponse.data)) {
        const userCourses = allCoursesResponse.data.filter(
          course => course.owner?.id.toString() === userId || course.ownerId?.toString() === userId
        );
        console.log('Filtered user courses:', userCourses);
        return { data: userCourses };
      }
    }
    
    return response;
  } catch (error) {
    console.error('Error fetching courses:', error);
    throw error;
  }
};

export const getCourseById = async (id) => {
  return await apiClient.get(`/courses/${id}`);
};

export const evaluateFlashCardAnswer = async (courseId, { question, expectedAnswer, userAnswer }) => {
  return await apiClient.post(`/courses/${courseId}/flashcards/evaluate`, {
    question,
    expectedAnswer,
    userAnswer,
  });
};

export const createCourse = async (courseData) => {
  const userId = localStorage.getItem('userId');
  if (!userId) {
    console.error('No user ID found in localStorage');
    throw new Error('User ID not found. Please log in again.');
  }

  console.log('Creating course with user ID:', userId);
  
  // Try multiple formats to match backend expectations
  const data = {
    ...courseData,
    // Format 1: ownerId as property
    ownerId: userId,
    // Format 2: owner object with id
    owner: { id: userId }
  };
  
  console.log('Sending course creation request with data:', data);
  
  try {
    const response = await apiClient.post('/courses', data);
    console.log('Course creation successful:', response);
    return response;
  } catch (error) {
    console.error('Course creation failed:', error);
    throw error;
  }
};

export const updateCourse = async (id, courseData) => {
  return await apiClient.put(`/courses/${id}`, courseData);
};

export const deleteCourse = async (id) => {
  return await apiClient.delete(`/courses/${id}`);
};

export const uploadPdf = async (courseId, file) => {
  const formData = new FormData();
  formData.append('file', file);
  
  return await apiClient.post(`/courses/${courseId}/upload-pdf`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

export const uploadDocx = async (courseId, file) => {
  const formData = new FormData();
  formData.append('file', file);
  
  return await apiClient.post(`/courses/${courseId}/upload-docx`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

export const uploadVideo = async (courseId, file) => {
  const formData = new FormData();
  formData.append('file', file);
  
  return await apiClient.post(`/courses/${courseId}/upload-video`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

export const addYoutubeVideo = async (courseId, youtubeUrl) => {
  return await apiClient.post(`/courses/${courseId}/add-youtube-video`, { youtubeUrl });
};

export const getCourseTasks = async (courseId) => {
  return await apiClient.get(`/courses/${courseId}/tasks`);
};

export const getUploadedFiles = async (courseId) => {
  return await apiClient.get(`/courses/${courseId}/uploaded-files`);
};

export const getTaskStatus = async (courseId, taskId) => {
  return await apiClient.get(`/courses/${courseId}/task-status/${taskId}`);
};

export const deleteCoursePdfFile = async (courseId, fileId) => {
  return await apiClient.delete(`/courses/${courseId}/materials/pdf/${fileId}`);
};

export const deleteCourseDocxFile = async (courseId, fileId) => {
  return await apiClient.delete(`/courses/${courseId}/materials/docx/${fileId}`);
};

export const deleteCourseVideoFile = async (courseId, fileId) => {
  return await apiClient.delete(`/courses/${courseId}/materials/video/${fileId}`);
};

export const deleteCourseXmlFile = async (courseId, fileId) => {
  return await apiClient.delete(`/courses/${courseId}/materials/xml/${fileId}`);
};

export const getQuizQuestions = async (courseId, fileId) => {
  return await apiClient.get(`/courses/${courseId}/questions/${fileId}`);
};

export const uploadXml = async (courseId, file) => {
  const formData = new FormData();
  formData.append('file', file);
  return await apiClient.post(`/courses/${courseId}/upload-xml`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
};