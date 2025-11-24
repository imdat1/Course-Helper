// components/courses/CourseDetail.js
import React, { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { getCourseById } from '../../services/courseService';
import MaterialUpload from './MaterialUpload';
import UploadedFilesList from './UploadedFilesList';
import { FaComments, FaListAlt } from 'react-icons/fa';

const CourseDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [course, setCourse] = useState(null);
  // Track optimistic pending uploads so list shows instantly
  const [pendingFiles, setPendingFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchCourse();
  }, [id]);

  const fetchCourse = async () => {
    try {
      const response = await getCourseById(id);
      setCourse(response.data);
    } catch (err) {
      setError('Failed to load course details');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  // Called by MaterialUpload right before starting network calls
  const handleUploadStart = (filesMeta) => {
    // filesMeta: array of { tempId, filename, type }
    setPendingFiles(prev => {
      const merged = [...prev];
      filesMeta.forEach(m => {
        if (!merged.find(x => x.tempId === m.tempId)) {
          merged.push({...m});
        }
      });
      return merged;
    });
  };

  // After backend finishes and list refresh occurs, clear pending entry whose filename matches
  const reconcilePending = (loadedFiles) => {
    if (!Array.isArray(loadedFiles)) return;
    setPendingFiles(prev => prev.filter(p => !loadedFiles.some(f => f.filename === p.filename)));
  };

  if (loading) return <div className="text-center mt-5">Loading course details...</div>;
  if (error) return <div className="alert alert-danger mt-3">{error}</div>;
  if (!course) return <div className="alert alert-warning mt-3">Course not found</div>;

  return (
    <div className="container mt-4">
      <div className="row">
        <div className="col-md-8">
          <div className="card mb-4">
            <div className="card-body">
              <h5 className="card-title">Description</h5>
              <p className="card-text">{course.description || 'No description provided.'}</p>
            </div>
          </div>

          {course.flashCards && course.flashCards.length > 0 && (
            <div className="card mb-4">
              <div className="card-body">
                <h6><FaListAlt className="me-2" /> Flash Cards ({course.flashCards.length})</h6>
                <div className="list-group mt-2">
                  {course.flashCards.slice(0, 3).map((card, index) => (
                    <div key={index} className="list-group-item">
                      <h6>{card.question}</h6>
                      <p className="mb-0 text-muted">{card.answer}</p>
                    </div>
                  ))}
                  {course.flashCards.length > 3 && (
                    <div className="list-group-item text-center">
                      <Link to={`/courses/${id}/flashcards`}>View all {course.flashCards.length} flash cards</Link>
                    </div>
                  )}
                </div>
              </div>
            </div>
          )}

          <div className="d-flex mb-4">
            <Link to={`/courses/${id}/chat`} className="btn btn-primary me-2">
              <FaComments className="me-2" /> Chat with Course
            </Link>
            <Link to="/courses" className="btn btn-secondary">Back to Courses</Link>
          </div>
        </div>

        <div className="col-md-4">
          <UploadedFilesList
            courseId={id}
            onChange={(loaded) => { fetchCourse(); reconcilePending(loaded); }}
            pendingFiles={pendingFiles}
          />
          <MaterialUpload
            courseId={id}
            onUploadComplete={() => fetchCourse()}
            onUploadStart={handleUploadStart}
          />
        </div>
      </div>
    </div>
  );
};

export default CourseDetail;