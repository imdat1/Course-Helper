import React, { useEffect, useState } from 'react';
import parse from 'html-react-parser';
import { useParams, Link } from 'react-router-dom';
import { getQuizQuestions, getUploadedFiles, getTaskStatus } from '../../services/courseService';
import { startSimilarQuizExport, downloadExportedQuiz, getQuizExportTasks } from '../../services/quizExportService';
import { replacePluginImagesWithBase64 } from '../../utils/htmlImageUtils';

// Placeholder pattern used in Moodle-like cloze syntax
const PLACEHOLDER_PATTERN = "\\{\\d+:[A-Z]+:[^}]+\\}"; // no flags here
const placeholderDetectRe = new RegExp(PLACEHOLDER_PATTERN); // detection only, no 'g'

function parseAnswersJsonEnhanced(answersJson) {
  let raw;
  try { raw = JSON.parse(answersJson); } catch { return { answers: [], type: null, topLevelReasoning: null, guidelines: null }; }
  if (Array.isArray(raw)) {
    return { answers: raw.filter(obj => obj && !obj.guidelines), type: 'legacy', topLevelReasoning: null, guidelines: null };
  }
  const type = raw.type || null;
  const guidelines = raw.guidelines || null;
  const topLevelReasoning = type === 'table' ? (raw.ai_reasoning || null) : null;
  const answers = Array.isArray(raw.questions) ? raw.questions : [];
  return { answers, type, topLevelReasoning, guidelines };
}

const MultipleChoiceInput = ({ placeholder, answerObj, onSubmit, submitted }) => {
  const inside = placeholder.slice(1, -1);
  const parts = inside.split(':');
  const rest = parts.slice(2).join(':');
  const optionParts = rest.split('~');
  const options = optionParts.map(opt => {
    const match = opt.match(/^%(\d+)%(.+)$/);
    if (match) return { text: match[2], correctFlag: match[1] === '100' };
    // Support MCS format where correct answers are prefixed with '=' (e.g. =A~B~C)
    if (opt.startsWith('=')) return { text: opt.slice(1), correctFlag: true };
    return { text: opt, correctFlag: false };
  });
  const [selected, setSelected] = useState('');
  return (
    <span className="d-inline-flex align-items-center ms-2 me-2">
      <select disabled={submitted} value={selected} onChange={e=>setSelected(e.target.value)} className="form-select form-select-sm me-2" style={{width:'auto'}}>
        <option value="">Select</option>
        {options.map((o,i)=><option key={i} value={o.text}>{o.text}</option>)}
      </select>
      {!submitted && <button type="button" className="btn btn-sm btn-outline-primary" onClick={()=>onSubmit(selected)}>Submit</button>}
    </span>
  );
};

const NumericalInput = ({ answerObj, onSubmit, submitted }) => {
  const [val,setVal] = useState('');
  return (
    <span className="d-inline-flex align-items-center ms-2 me-2">
      <input disabled={submitted} type="number" value={val} onChange={e=>setVal(e.target.value)} className="form-control form-control-sm me-2" style={{width:'6rem'}} />
      {!submitted && <button type="button" className="btn btn-sm btn-outline-primary" onClick={()=>onSubmit(val)}>Submit</button>}
    </span>
  );
};

// Generic text input for placeholder types we don't explicitly parse (e.g. SHORTANSWER, TABLE cells, etc.)
const GenericTextInput = ({ onSubmit, submitted, answerObj }) => {
  const [val, setVal] = useState('');
  return (
    <span className="d-inline-flex align-items-center ms-2 me-2">
      <input disabled={submitted} type="text" value={val} onChange={e=>setVal(e.target.value)} className="form-control form-control-sm me-2" style={{width:'12rem'}} />
      {!submitted && <button type="button" className="btn btn-sm btn-outline-primary" onClick={()=>onSubmit(val)}>Submit</button>}
    </span>
  );
};

const QuizQuestion = ({ question }) => {
  const { answers, type, topLevelReasoning, guidelines } = parseAnswersJsonEnhanced(question.answersJson);
  const htmlWithImages = replacePluginImagesWithBase64(question.questionText, question.imagesJson);
  const [submittedMap, setSubmittedMap] = useState({});
  const [correctnessMap, setCorrectnessMap] = useState({}); // idx -> true/false
  const [showTopReasoning, setShowTopReasoning] = useState(false);

  const submit = (idx, value, answerObj) => {
    setSubmittedMap(prev=>({ ...prev, [idx]: value }));
    if (answerObj && answerObj.correct_answer) {
      const user = ('' + value).trim().toLowerCase();
      const correct = ('' + answerObj.correct_answer).trim().toLowerCase();
      setCorrectnessMap(prev=>({ ...prev, [idx]: user === correct }));
    }
  };
  const resetAnswers = () => setSubmittedMap({});

  // Render placeholders in-place within the parsed HTML to preserve table structure
  let phCounter = 0;
  const content = parse(htmlWithImages, {
    replace: (node) => {
      if (node.type === 'text' && node.data && placeholderDetectRe.test(node.data)) {
        const str = node.data;
        const re = new RegExp(PLACEHOLDER_PATTERN, 'g');
        const out = [];
        let last = 0;
        let m;
        while ((m = re.exec(str)) !== null) {
          if (m.index > last) out.push(str.slice(last, m.index));
          const idx = phCounter++;
          const ph = m[0];
          const answerObj = answers[idx] || null;
          const submitted = submittedMap[idx] !== undefined;

          let inputEl = null;
          if (ph.includes(':MC:') || ph.includes(':MCS:')) {
            inputEl = <MultipleChoiceInput placeholder={ph} answerObj={answerObj} submitted={submitted} onSubmit={(val)=>submit(idx,val,answerObj)} />;
          } else if (ph.includes(':NUMERICAL:')) {
            inputEl = <NumericalInput answerObj={answerObj} submitted={submitted} onSubmit={(val)=>submit(idx,val,answerObj)} />;
          } else {
            inputEl = <GenericTextInput answerObj={answerObj} submitted={submitted} onSubmit={(val)=>submit(idx,val,answerObj)} />;
          }

          out.push(
            <React.Fragment key={`ph-${idx}`}>
              {inputEl}
              {submitted && answerObj && answerObj.correct_answer && (
                correctnessMap[idx] ? <span className="badge bg-success ms-2">Correct</span> : <span className="badge bg-danger ms-2">Incorrect</span>
              )}
              {submitted && answerObj && answerObj.ai_reasoning && type !== 'table' && (
                <div className="ai-answer mt-2">{answerObj.ai_reasoning}</div>
              )}
            </React.Fragment>
          );
          last = m.index + m[0].length;
        }
        if (last < str.length) out.push(str.slice(last));
        return <>{out}</>;
      }
      return undefined;
    }
  });

  return (
    <div className="card mb-3 question-card">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-start">
          <h6 className="card-title">Question</h6>
          <div className="btn-group">
            <button type="button" className="btn btn-sm btn-outline-secondary" onClick={resetAnswers}>Reset Answers</button>
            {type === 'table' && topLevelReasoning && (
              <button type="button" className="btn btn-sm btn-outline-info" onClick={()=>setShowTopReasoning(s=>!s)}>{showTopReasoning ? 'Hide Reasoning' : 'Show Reasoning'}</button>
            )}
          </div>
        </div>
        {guidelines && <div className="alert alert-secondary py-1 mb-2"><small><strong>Guidelines:</strong> {guidelines}</small></div>}
        {type === 'table' && showTopReasoning && topLevelReasoning && (
          <div className="ai-answer mb-2">{topLevelReasoning}</div>
        )}
        <div className="question-content">{content}</div>
      </div>
    </div>
  );
};

const QuizView = () => {
  const { id: courseId, fileId } = useParams();
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [quizTitle, setQuizTitle] = useState('');
  const [exports, setExports] = useState([]); // [{taskId, status}]

  useEffect(()=>{
    const load = async () => {
      setLoading(true); setError('');
      try {
        const resp = await getQuizQuestions(courseId, fileId);
        setQuestions(Array.isArray(resp.data) ? resp.data : []);
        // Resolve quiz title
        try {
          const filesResp = await getUploadedFiles(courseId);
          const files = Array.isArray(filesResp.data) ? filesResp.data : [];
          const match = files.find(f => (''+f.fileId) === (''+fileId));
          if (match && match.filename) setQuizTitle(match.filename); else setQuizTitle('');
        } catch {}
        // Load existing export tasks for persistence
        try {
          const exportsResp = await getQuizExportTasks(courseId, fileId);
          const list = Array.isArray(exportsResp.data) ? exportsResp.data : [];
          const mapped = list.map(t => ({ taskId: t.taskId, status: t.status }));
          setExports(mapped);
          // Resume polling for any still pending
          mapped.filter(x => x.status === 'PENDING').forEach(x => pollStatus(x.taskId));
        } catch (e) {
          // Non-critical
        }
      } catch (e) { setError('Failed to load quiz questions'); }
      finally { setLoading(false); }
    };
    load();
  }, [courseId, fileId]);

  const startExport = async () => {
    const ok = window.confirm('A new AI-generated quiz will be generated. Are you sure?');
    if (!ok) return;
    try {
      const resp = await startSimilarQuizExport(courseId, fileId);
      const taskId = resp.data?.taskId;
      if (taskId) {
        setExports(prev => [...prev, { taskId, status: 'PENDING' }]);
        pollStatus(taskId);
      }
    } catch (e) {
      console.error('Failed to start export', e);
      alert('Failed to start quiz generation.');
    }
  };

  const pollStatus = async (taskId) => {
    const poll = async () => {
      try {
        const res = await getTaskStatus(courseId, taskId);
        const status = res.data?.status || 'PENDING';
        setExports(prev => prev.map(x => x.taskId === taskId ? { ...x, status } : x));
        if (status === 'SUCCESS' || status === 'FAILURE') return; // stop
      } catch (e) {
        // keep polling on transient errors
      }
      setTimeout(poll, 3000);
    };
    poll();
  };

  const downloadExport = async (taskId) => {
    try {
      const resp = await downloadExportedQuiz(courseId, taskId);
      if (resp && resp.data) {
        const blob = new Blob([resp.data], { type: 'application/xml' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `ai_quiz_${taskId}.xml`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        window.URL.revokeObjectURL(url);
      } else {
        alert('Export not ready yet.');
      }
    } catch (e) {
      alert('Export not ready yet.');
    }
  };

  return (
    <div className="container mt-4 quiz-container">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h3 className="mb-0">{quizTitle ? `Quiz: ${quizTitle}` : `Quiz`}</h3>
        <div className="d-flex gap-2">
          <button type="button" className="btn btn-primary" onClick={startExport}>Create similar quiz</button>
          <Link to={`/courses/${courseId}`} className="btn btn-outline-secondary">Back to Course</Link>
        </div>
      </div>
      {exports.length > 0 && (
        <div className="mb-3">
          <h6>AI Quiz Generations</h6>
          <ul className="list-group">
            {exports.map(x => (
              <li key={x.taskId} className="list-group-item d-flex justify-content-between align-items-center">
                <span>{x.status === 'PENDING' ? 'Quiz generation in progress...' : `Status: ${x.status}`}</span>
                {x.status === 'SUCCESS' && (
                  <button className="btn btn-sm btn-success" onClick={() => downloadExport(x.taskId)}>Download</button>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
      {error && <div className="alert alert-danger">{error}</div>}
      {loading && <div>Loading quiz...</div>}
      {!loading && questions.length === 0 && <div className="alert alert-info">No questions found.</div>}
      {questions.map(q => <QuizQuestion key={q.id} question={q} />)}
    </div>
  );
};

export default QuizView;