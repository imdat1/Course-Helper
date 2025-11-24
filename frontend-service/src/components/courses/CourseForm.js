// components/courses/CourseForm.js
import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { createCourse, getCourseById, updateCourse } from '../../services/courseService';

const CourseForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [course, setCourse] = useState({
    title: '',
    description: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const isEdit = !!id;

  // Add this inside your component, before the return statement
useEffect(() => {
  const testApi = async () => {
    try {
      const token = localStorage.getItem('authToken');
      console.log('Testing direct API with token:', token ? 'Token exists' : 'No token');
      
      // Test a simple GET request first
      const testResponse = await fetch('http://localhost:8080/api/courses', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      console.log('Test API status:', testResponse.status);
      const data = await testResponse.text();
      console.log('Test API response:', data.substring(0, 100)); // First 100 chars
      
      if (data.includes('<!DOCTYPE html>')) {
        console.error('Received HTML instead of JSON - likely an auth issue');
      }
    } catch (err) {
      console.error('Test API error:', err);
    }
  };
  
  testApi();
}, []);

  const fetchCourse = async () => {
    try {
      const response = await getCourseById(id);
      setCourse({
        title: response.data.title,
        description: response.data.description
      });
    } catch (err) {
      setError('Failed to load course');
      console.error(err);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setCourse({
      ...course,
      [name]: value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    const userId = localStorage.getItem('userId');
    console.log('Current user ID:', userId);

    try {
        console.log('Attempting to create course with data:', course);
        
        if (isEdit) {
        const response = await updateCourse(id, course);
        console.log('Course update response:', response);
        } else {
        const response = await createCourse(course);
        console.log('Course creation response:', response);
        
        if (response && response.data) {
            console.log('Created course:', response.data);
        }
        }
        
        // Navigate after success
        navigate('/courses');
    } catch (err) {
        console.error('Error in form submission:', err);
        // Detailed error analysis
        if (err.response) {
        console.error('Response error data:', err.response.data);
        console.error('Response status:', err.response.status);
        setError(`Server error (${err.response.status}): ${err.response.data?.message || 'Unknown error'}`);
        } else if (err.request) {
        console.error('No response received');
        setError('No response from server. Please check your network connection.');
        } else {
        console.error('Error message:', err.message);
        setError(`Error: ${err.message}`);
        }
    } finally {
        setLoading(false);
    }
    };

  return (
    <div className="container mt-4">
      <h2>{isEdit ? 'Edit Course' : 'Create New Course'}</h2>
      {error && <div className="alert alert-danger">{error}</div>}
      
      <form onSubmit={handleSubmit}>
        <div className="mb-3">
          <label htmlFor="title" className="form-label">Title</label>
          <input
            type="text"
            className="form-control"
            id="title"
            name="title"
            value={course.title}
            onChange={handleChange}
            required
          />
        </div>
        
        <div className="mb-3">
          <label htmlFor="description" className="form-label">Description</label>
          <textarea
            className="form-control"
            id="description"
            name="description"
            rows="4"
            value={course.description}
            onChange={handleChange}
          />
        </div>
        
        <div className="mb-3">
          <button 
            type="submit" 
            className="btn btn-primary me-2"
            disabled={loading}
          >
            {loading ? 'Saving...' : 'Save Course'}
          </button>
          <button 
            type="button" 
            className="btn btn-secondary"
            onClick={() => navigate('/courses')}
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
};

export default CourseForm;