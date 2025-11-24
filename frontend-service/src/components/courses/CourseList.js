import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getCourses, deleteCourse } from '../../services/courseService';
import { FaPlus, FaEdit, FaTrash, FaComments, FaSync } from 'react-icons/fa';

const CourseList = () => {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchCourses();
  }, []);

  // Add this debugging effect
  useEffect(() => {
    console.log('Courses state updated:', courses);
  }, [courses]);

  const fetchCourses = async () => {
    setLoading(true);
    try {
      // Log user info for debugging
      const userId = localStorage.getItem('userId');
      console.log('Fetching courses for user ID:', userId);
      
      const response = await getCourses();
      console.log('Raw API response:', response);
      
      if (Array.isArray(response.data)) {
        console.log('Setting courses from array response:', response.data);
        setCourses(response.data);
      } else if (response.data && typeof response.data === 'object') {
        console.log('Response is an object, looking for arrays inside:', response.data);
        // Try to find arrays in the response
        const possibleArrays = Object.entries(response.data)
          .filter(([key, value]) => Array.isArray(value))
          .map(([key, value]) => ({ key, value }));
        
        console.log('Possible arrays found:', possibleArrays);
        
        if (possibleArrays.length > 0) {
          // Use the first array found
          console.log(`Using array from property "${possibleArrays[0].key}"`, possibleArrays[0].value);
          setCourses(possibleArrays[0].value);
        } else {
          // If it's an empty object but not null, assume it means no courses
          console.log('No arrays found in response, setting empty courses array');
          setCourses([]);
        }
      } else {
        // Handle unexpected response
        console.error('Unexpected API response format:', response.data);
        setCourses([]);
      }
    } catch (err) {
      console.error('Error fetching courses:', err);
      setError('Failed to load courses: ' + (err.message || ''));
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this course?')) {
      try {
        await deleteCourse(id);
        setCourses(courses.filter(course => course.id !== id));
      } catch (err) {
        setError('Failed to delete course');
        console.error(err);
      }
    }
  };

  // Add a manual refresh button
  const handleRefresh = () => {
    fetchCourses();
  };

  return (
    <div className="container mt-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>My Courses</h2>
        <div>
          <button onClick={handleRefresh} className="btn btn-outline-secondary me-2">
            <FaSync className="me-1" /> Refresh
          </button>
          <Link to="/courses/new" className="btn btn-success">
            <FaPlus className="me-1" /> New Course
          </Link>
        </div>
      </div>
      
      {loading ? (
        <div className="text-center my-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading courses...</span>
          </div>
          <p className="mt-2">Loading courses...</p>
        </div>
      ) : courses.length === 0 ? (
        <div className="alert alert-info">
          You don't have any courses yet. Create your first course to get started!
        </div>
      ) : (
        <div className="row">
          {courses.map(course => (
            <div key={course.id} className="col-md-4 mb-4">
              <div className="card h-100">
                <div className="card-body">
                  <h5 className="card-title">{course.title}</h5>
                  <p className="card-text">{course.description}</p>
                  <div className="d-flex mt-auto">
                    <Link to={`/courses/${course.id}`} className="btn btn-primary me-2">
                      View Details
                    </Link>
                    <Link to={`/courses/${course.id}/chat`} className="btn btn-info me-2">
                      <FaComments className="me-1" /> Chat
                    </Link>
                    <Link to={`/courses/${course.id}/edit`} className="btn btn-secondary me-2">
                      <FaEdit />
                    </Link>
                    <button 
                      className="btn btn-danger" 
                      onClick={() => handleDelete(course.id)}
                    >
                      <FaTrash />
                    </button>
                  </div>
                </div>
                <div className="card-footer text-muted">
                  Created: {course.createdAt ? new Date(course.createdAt).toLocaleDateString() : 'Unknown date'}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default CourseList;