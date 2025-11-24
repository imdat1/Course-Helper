// components/courses/UploadedFilesList.js
import React, { useEffect, useState, useCallback } from 'react';
import { getUploadedFiles, deleteCoursePdfFile, deleteCourseDocxFile, deleteCourseVideoFile, deleteCourseXmlFile, getTaskStatus } from '../../services/courseService';
import { FaFilePdf, FaFileWord, FaTrash, FaVideo, FaYoutube } from 'react-icons/fa';

const UploadedFilesList = ({ courseId, onChange, pendingFiles = [] }) => {
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const resp = await getUploadedFiles(courseId);
      const loaded = Array.isArray(resp.data) ? resp.data : [];
      setFiles(loaded);
      if (onChange) onChange(loaded);
    } catch (e) {
      console.error(e);
      setError('Failed to load uploaded files');
    } finally {
      setLoading(false);
    }
  }, [courseId]);

  useEffect(() => { load(); }, [load]);

  // Auto-refresh while any item is pending
  useEffect(() => {
    const hasPending = files.some(f => (f.status || '').toUpperCase() === 'PENDING');
    if (!hasPending) return;
    const t = setInterval(() => load(), 5000);
    return () => clearInterval(t);
  }, [files, load]);

  // Also auto-refresh when we have optimistic pending placeholders from uploads
  useEffect(() => {
    if (!pendingFiles || pendingFiles.length === 0) return;
    const t = setInterval(() => load(), 5000);
    return () => clearInterval(t);
  }, [pendingFiles, load]);

  // Additionally, poll task status by id for pending files to accelerate updates
  useEffect(() => {
    const pendingWithTasks = files.filter(f => (f.status || '').toUpperCase() === 'PENDING' && f.processingTaskId);
    if (pendingWithTasks.length === 0) return;
    let cancelled = false;
    const checkOnce = async () => {
      try {
        // Query all pending tasks in parallel; if any completes, reload list
        const results = await Promise.allSettled(pendingWithTasks.map(f => getTaskStatus(courseId, f.processingTaskId)));
        const anyDone = results.some(r => r.status === 'fulfilled' && r.value?.data && ['SUCCESS','FAILURE'].includes((r.value.data.status || '').toUpperCase()));
        if (!cancelled && anyDone) {
          await load();
        }
      } catch {}
    };
    const interval = setInterval(checkOnce, 4000);
    // Run an immediate check too
    checkOnce();
    return () => { cancelled = true; clearInterval(interval); };
  }, [files, courseId, load]);

  const onDelete = async (file) => {
    const isXml = (file.type || '').toUpperCase() === 'XML';
    const confirmed = window.confirm(
      isXml
        ? 'Are you sure you want to delete this quiz?'
        : 'Are you sure you want to delete this file? All flash cards will be deleted for the file as well'
    );
    if (!confirmed) return;
    try {
      if (file.type?.toUpperCase() === 'PDF') {
        await deleteCoursePdfFile(courseId, file.fileId);
      } else if (file.type?.toUpperCase() === 'DOCX') {
        await deleteCourseDocxFile(courseId, file.fileId);
      } else if (isXml) {
        await deleteCourseXmlFile(courseId, file.fileId);
      } else if (['FILE_VIDEO','YOUTUBE_VIDEO','VIDEO','YOUTUBE'].includes((file.type || '').toUpperCase())) {
        await deleteCourseVideoFile(courseId, file.fileId);
      }
      // Refresh list from server to ensure all references are gone
      await load();
    } catch (e) {
      console.error(e);
      setError('Failed to delete file');
    }
  };

  // Merge pending files that aren't yet in loaded list
  const mergedFiles = React.useMemo(() => {
    const existing = new Map((files || []).map(f => [f.filename + '|' + (f.type||''), f]));
    (pendingFiles || []).forEach(p => {
      const key = p.filename + '|' + (p.type || '');
      if (!existing.has(key)) {
        existing.set(key, { fileId: p.tempId, filename: p.filename, type: p.type, status: 'PENDING' });
      }
    });
    return Array.from(existing.values());
  }, [files, pendingFiles]);

  const pdfs = mergedFiles.filter(f => (f.type || '').toUpperCase() === 'PDF');
  const docxs = mergedFiles.filter(f => (f.type || '').toUpperCase() === 'DOCX');
  const videos = mergedFiles.filter(f => ['FILE_VIDEO','YOUTUBE_VIDEO','VIDEO','YOUTUBE'].includes((f.type || '').toUpperCase()))
  const xmls = mergedFiles.filter(f => (f.type || '').toUpperCase() === 'XML');

  return (
    <div className="card mb-4">
      <div className="card-header d-flex align-items-center justify-content-between">
        <h5 className="mb-0">Uploaded Documents</h5>
        <button className="btn btn-sm btn-outline-secondary" onClick={load} disabled={loading}>
          Refresh
        </button>
      </div>
      <div className="card-body">
        {error && <div className="alert alert-danger">{error}</div>}
        {loading && <div>Loading...</div>}
        {!loading && files.length === 0 && <div className="text-muted">No documents uploaded yet.</div>}

        {pdfs.length > 0 && (
          <div className="mb-3">
            <h6 className="mb-2"><FaFilePdf className="me-2 text-danger" />PDFs</h6>
            <ul className="list-group">
              {pdfs.map(f => (
                <li key={f.fileId} className="list-group-item d-flex align-items-center justify-content-between">
                  <span className="d-flex flex-column">
                    <span>{f.filename}</span>
                    {(f.status || '').toUpperCase() === 'PENDING' && <small className="text-warning">Processing... {f.processingTaskId ? `(task ${f.processingTaskId.slice(0,6)}…)` : ''}</small>}
                    {(f.status || '').toUpperCase() === 'FAILED' && <small className="text-danger">Failed</small>}
                  </span>
                  <button className="btn btn-sm btn-outline-danger" disabled={(f.status || '').toUpperCase()==='PENDING'} onClick={() => onDelete(f)}>
                    <FaTrash className="me-1" /> Delete
                  </button>
                </li>
              ))}
              {xmls.length > 0 && (
                <div className="mb-3">
                  <h6 className="mb-2">XML Quizzes</h6>
                  <ul className="list-group">
                    {xmls.map(f => (
                      <li key={f.fileId} className="list-group-item d-flex align-items-center justify-content-between">
                        <span className="d-flex flex-column">
                          <span>{f.filename}</span>
                          {(f.status || '').toUpperCase() === 'PENDING' && <small className="text-warning">Processing... {f.processingTaskId ? `(task ${f.processingTaskId.slice(0,6)}…)` : ''}</small>}
                          {(f.status || '').toUpperCase() === 'FAILED' && <small className="text-danger">Failed</small>}
                        </span>
                        <div className="d-flex align-items-center">
                          <button className="btn btn-sm btn-outline-primary me-2" disabled={(f.status||'').toUpperCase()!=='READY'} onClick={() => window.location.href = `/courses/${courseId}/quizzes/${f.fileId}`}>Solve</button>
                          <button className="btn btn-sm btn-outline-danger" disabled={(f.status || '').toUpperCase()==='PENDING'} onClick={() => onDelete(f)}>
                            <FaTrash className="me-1" /> Delete
                          </button>
                        </div>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </ul>
          </div>
        )}

        {docxs.length > 0 && (
          <div className="mb-2">
            <h6 className="mb-2"><FaFileWord className="me-2 text-primary" />DOCXs</h6>
            <ul className="list-group">
              {docxs.map(f => (
                <li key={f.fileId} className="list-group-item d-flex align-items-center justify-content-between">
                  <span className="d-flex flex-column">
                    <span>{f.filename}</span>
                    {(f.status || '').toUpperCase() === 'PENDING' && <small className="text-warning">Processing... {f.processingTaskId ? `(task ${f.processingTaskId.slice(0,6)}…)` : ''}</small>}
                    {(f.status || '').toUpperCase() === 'FAILED' && <small className="text-danger">Failed</small>}
                  </span>
                  <button className="btn btn-sm btn-outline-danger" disabled={(f.status || '').toUpperCase()==='PENDING'} onClick={() => onDelete(f)}>
                    <FaTrash className="me-1" /> Delete
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}

        {videos.length > 0 && (
          <div className="mt-3">
            <h6 className="mb-2"><FaVideo className="me-2 text-secondary" />Videos</h6>
            <ul className="list-group">
              {videos.map(f => (
                <li key={f.fileId} className="list-group-item d-flex align-items-center justify-content-between">
                  <span className="d-flex flex-column">
                    <span className="d-flex align-items-center">
                      {(f.type || '').toUpperCase().includes('YOUTUBE') ? <FaYoutube className="me-2 text-danger" /> : <FaVideo className="me-2 text-secondary" />}
                      {f.filename}
                    </span>
                    {(f.status || '').toUpperCase() === 'PENDING' && <small className="text-warning">Processing... {f.processingTaskId ? `(task ${f.processingTaskId.slice(0,6)}…)` : ''}</small>}
                    {(f.status || '').toUpperCase() === 'FAILED' && <small className="text-danger">Failed</small>}
                  </span>
                  <button className="btn btn-sm btn-outline-danger" disabled={(f.status || '').toUpperCase()==='PENDING'} onClick={() => onDelete(f)}>
                    <FaTrash className="me-1" /> Delete
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
};

export default UploadedFilesList;
