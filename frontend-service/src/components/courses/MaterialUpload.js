// components/courses/MaterialUpload.js
import React, { useState } from 'react';
import { uploadPdf, uploadDocx, uploadVideo, addYoutubeVideo, uploadXml } from '../../services/courseService';
import { FaFilePdf, FaFileWord, FaVideo, FaYoutube } from 'react-icons/fa';

const MaterialUpload = ({ courseId, onUploadComplete, onUploadStart }) => {
  const [pdfFiles, setPdfFiles] = useState([]);
  const [docxFiles, setDocxFiles] = useState([]);
  const [videoFile, setVideoFile] = useState(null);
  const [xmlFiles, setXmlFiles] = useState([]);
  const [youtubeUrl, setYoutubeUrl] = useState('');
  const [loadingPdf, setLoadingPdf] = useState(false);
  const [loadingDocx, setLoadingDocx] = useState(false);
  const [loadingXml, setLoadingXml] = useState(false);
  const [loadingVideo, setLoadingVideo] = useState(false);
  const [loadingYoutube, setLoadingYoutube] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  // Note: File listing and task status UI have been moved to UploadedFilesList.
  // This component focuses purely on uploading materials.

  const handlePdfUpload = async (e) => {
    e.preventDefault();
    if (!pdfFiles || pdfFiles.length === 0) return;
    
    setLoadingPdf(true);
    setError('');
    setSuccess('');
    
    try {
      // optimistic placeholders
      if (onUploadStart) {
        onUploadStart(pdfFiles.map((f, idx) => ({ tempId: `pdf-${Date.now()}-${idx}`, filename: f.name, type: 'PDF' })));
      }
      // Fire uploads concurrently so each file enqueues its own backend task immediately
      await Promise.allSettled(pdfFiles.map(f => uploadPdf(courseId, f)));
      if (onUploadComplete) onUploadComplete();
      setSuccess(`Uploaded ${pdfFiles.length} PDF file(s)`);
      setPdfFiles([]);
    } catch (err) {
      setError('Failed to upload PDF');
      console.error(err);
    } finally {
      setLoadingPdf(false);
    }
  };

  const handleDocxUpload = async (e) => {
    e.preventDefault();
    if (!docxFiles || docxFiles.length === 0) return;
    
    setLoadingDocx(true);
    setError('');
    setSuccess('');
    
    try {
      if (onUploadStart) {
        onUploadStart(docxFiles.map((f, idx) => ({ tempId: `docx-${Date.now()}-${idx}`, filename: f.name, type: 'DOCX' })));
      }
      await Promise.allSettled(docxFiles.map(f => uploadDocx(courseId, f)));
      if (onUploadComplete) onUploadComplete();
      setSuccess(`Uploaded ${docxFiles.length} DOCX file(s)`);
      setDocxFiles([]);
    } catch (err) {
      setError('Failed to upload DOCX');
      console.error(err);
    } finally {
      setLoadingDocx(false);
    }
  };

  const handleVideoUpload = async (e) => {
    e.preventDefault();
    if (!videoFile) return;
    
    setLoadingVideo(true);
    setError('');
    setSuccess('');
    
    try {
      if (onUploadStart) {
        onUploadStart([{ tempId: `video-${Date.now()}`, filename: videoFile.name, type: 'FILE_VIDEO' }]);
      }
      await uploadVideo(courseId, videoFile);
      setSuccess('Video uploaded successfully');
      setVideoFile(null);
      if (onUploadComplete) onUploadComplete();
    } catch (err) {
      setError('Failed to upload video');
      console.error(err);
    } finally {
      setLoadingVideo(false);
    }
  };

  const handleXmlUpload = async (e) => {
    e.preventDefault();
    if (!xmlFiles || xmlFiles.length === 0) return;
    setLoadingXml(true); setError(''); setSuccess('');
    try {
      if (onUploadStart) {
        onUploadStart(xmlFiles.map((f, idx) => ({ tempId: `xml-${Date.now()}-${idx}`, filename: f.name, type: 'XML' })));
      }
      await Promise.allSettled(xmlFiles.map(f => uploadXml(courseId, f)));
      if (onUploadComplete) onUploadComplete();
      setSuccess(`Uploaded ${xmlFiles.length} XML file(s)`);
      setXmlFiles([]);
    } catch (err) {
      setError('Failed to upload XML');
      console.error(err);
    } finally { setLoadingXml(false); }
  };

  const handleYoutubeAdd = async (e) => {
    e.preventDefault();
    if (!youtubeUrl) return;
    
    setLoadingYoutube(true);
    setError('');
    setSuccess('');
    
    try {
      if (onUploadStart) {
        onUploadStart([{ tempId: `yt-${Date.now()}`, filename: youtubeUrl, type: 'YOUTUBE_VIDEO' }]);
      }
      await addYoutubeVideo(courseId, youtubeUrl);
      setSuccess('YouTube video added successfully');
      setYoutubeUrl('');
      if (onUploadComplete) onUploadComplete();
    } catch (err) {
      setError('Failed to add YouTube video');
      console.error(err);
    } finally {
      setLoadingYoutube(false);
    }
  };

  return (
    <div className="card">
      <div className="card-header">
        <h5>Add Course Materials</h5>
      </div>
      <div className="card-body">
        {error && <div className="alert alert-danger">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}
        {/* File listing and task status are displayed in UploadedFilesList component */}
        
        <div className="mb-4">
          <h6><FaFilePdf className="me-2" /> Upload PDF</h6>
          <form onSubmit={handlePdfUpload}>
            <div className="mb-3">
              <input 
                type="file" 
                className="form-control" 
                accept=".pdf"
                multiple
                onChange={(e) => setPdfFiles(Array.from(e.target.files))}
              />
            </div>
            <button 
              type="submit" 
              className="btn btn-primary"
              disabled={loadingPdf || !pdfFiles || pdfFiles.length === 0}
            >
              {loadingPdf ? 'Uploading...' : 'Upload PDF'}
            </button>
          </form>
        </div>

        <div className="mb-4">
          <h6>Upload XML</h6>
          <form onSubmit={handleXmlUpload}>
            <div className="mb-3">
              <input
                type="file"
                className="form-control"
                accept=".xml"
                multiple
                onChange={(e) => setXmlFiles(Array.from(e.target.files))}
              />
            </div>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={loadingXml || !xmlFiles || xmlFiles.length === 0}
            >
              {loadingXml ? 'Uploading...' : 'Upload XML'}
            </button>
          </form>
        </div>
        
        <div className="mb-4">
          <h6><FaFileWord className="me-2" /> Upload DOCX</h6>
          <form onSubmit={handleDocxUpload}>
            <div className="mb-3">
              <input 
                type="file" 
                className="form-control" 
                accept=".docx"
                multiple
                onChange={(e) => setDocxFiles(Array.from(e.target.files))}
              />
            </div>
            <button 
              type="submit" 
              className="btn btn-primary"
              disabled={loadingDocx || !docxFiles || docxFiles.length === 0}
            >
              {loadingDocx ? 'Uploading...' : 'Upload DOCX'}
            </button>
          </form>
        </div>
        
        <div className="mb-4">
          <h6><FaVideo className="me-2" /> Upload Video</h6>
          <form onSubmit={handleVideoUpload}>
            <div className="mb-3">
              <input 
                type="file" 
                className="form-control" 
                accept="video/mp4"
                onChange={(e) => setVideoFile(e.target.files[0])}
                required
              />
            </div>
            <button 
              type="submit" 
              className="btn btn-primary"
              disabled={loadingVideo || !videoFile}
            >
              {loadingVideo ? 'Uploading...' : 'Upload Video'}
            </button>
          </form>
        </div>
        
        <div className="mb-4">
          <h6><FaYoutube className="me-2" /> Add YouTube Video</h6>
          <form onSubmit={handleYoutubeAdd}>
            <div className="mb-3">
              <input 
                type="text" 
                className="form-control" 
                placeholder="YouTube URL"
                value={youtubeUrl}
                onChange={(e) => setYoutubeUrl(e.target.value)}
                required
              />
            </div>
            <button 
              type="submit" 
              className="btn btn-primary"
              disabled={loadingYoutube || !youtubeUrl}
            >
              {loadingYoutube ? 'Adding...' : 'Add YouTube Video'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};

export default MaterialUpload;