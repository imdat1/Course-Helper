import React, { useState } from 'react';
import { replacePluginImagesWithBase64 } from '../../utils/htmlImageUtils';

// Utility to parse placeholders like {1:MC:%100%optionA~%0%optionB}
const placeholderRegex = /\{\d+:[A-Z]+:[^}]+\}/g;

function parseAnswersJsonEnhanced(answersJson) {
  let raw;
  try { raw = JSON.parse(answersJson); } catch { return { answers: [], type: null, topLevelReasoning: null, guidelines: null }; }
  // Legacy format: array of answer objects
  if (Array.isArray(raw)) {
    return { answers: raw.filter(obj => obj && !obj.guidelines), type: 'legacy', topLevelReasoning: null, guidelines: null };
  }
  // New format: dict with type, guidelines, ai_reasoning, questions[]
  const type = raw.type || null;
  const guidelines = raw.guidelines || null;
  const topLevelReasoning = type === 'table' ? (raw.ai_reasoning || null) : null;
  const answers = Array.isArray(raw.questions) ? raw.questions : [];
  return { answers, type, topLevelReasoning, guidelines };
}

const MultipleChoiceInput = ({ placeholder, answerObj, onSubmit, submitted }) => {
  // Extract options
  const raw = placeholder;
  const inside = raw.slice(1, -1); // remove {}
  const parts = inside.split(':');
  const type = parts[1];
  const rest = parts.slice(2).join(':');
  const optionParts = rest.split('~');
  const options = optionParts.map(opt => {
    // %100%correctText or %0%wrongText
    const match = opt.match(/^%(\d+)%(.+)$/);
    if (match) {
      return { text: match[2], correctFlag: match[1] === '100' };
    }
    return { text: opt, correctFlag: false };
  });
  const [selected, setSelected] = useState('');
  return (
    <span className="d-inline-flex align-items-center ms-2 me-2">
      <select disabled={submitted} value={selected} onChange={e => setSelected(e.target.value)} className="form-select form-select-sm me-2" style={{width:'auto'}}>
        <option value="">Select</option>
        {options.map((o,i) => <option key={i} value={o.text}>{o.text}</option>)}
      </select>
      {!submitted && <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => onSubmit(selected)}>Submit</button>}
      {submitted && answerObj && <span className="badge bg-info ms-2">{answerObj.ai_reasoning}</span>}
    </span>
  );
};

const NumericalInput = ({ placeholder, answerObj, onSubmit, submitted }) => {
  const [val, setVal] = useState('');
  return (
    <span className="d-inline-flex align-items-center ms-2 me-2">
      <input disabled={submitted} type="number" value={val} onChange={e=>setVal(e.target.value)} className="form-control form-control-sm me-2" style={{width:'6rem'}} />
      {!submitted && <button type="button" className="btn btn-sm btn-outline-primary" onClick={() => onSubmit(val)}>Submit</button>}
      {submitted && answerObj && <span className="badge bg-info ms-2">{answerObj.ai_reasoning}</span>}
    </span>
  );
};

const renderWithInteractiveInputs = (html, answers) => {
  const placeholders = html.match(placeholderRegex) || [];
  const segments = html.split(placeholderRegex);
  // answers assumed in order corresponding to placeholders
  return { segments, placeholders };
};

const CourseQuestion = ({ question }) => {
  const { answers, type, topLevelReasoning, guidelines } = parseAnswersJsonEnhanced(question.answersJson);
  const htmlWithImages = replacePluginImagesWithBase64(question.questionText, question.imagesJson);
  const { segments, placeholders } = renderWithInteractiveInputs(htmlWithImages, answers);
  const [submittedMap, setSubmittedMap] = useState({});
  const [showTopReasoning, setShowTopReasoning] = useState(false);

  const handleSubmit = (index, value) => {
    setSubmittedMap(prev => ({ ...prev, [index]: value }));
  };

  return (
    <div className="card mb-3">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-start">
          <h6 className="card-title">Question</h6>
          {type === 'table' && topLevelReasoning && (
            <button type="button" className="btn btn-sm btn-outline-info" onClick={() => setShowTopReasoning(s=>!s)}>
              {showTopReasoning ? 'Hide Reasoning' : 'Show Reasoning'}
            </button>
          )}
        </div>
        {guidelines && <div className="alert alert-secondary py-1 mb-2"><small><strong>Guidelines:</strong> {guidelines}</small></div>}
        {type === 'table' && showTopReasoning && topLevelReasoning && (
          <div className="mb-2"><span className="badge bg-info">{topLevelReasoning}</span></div>
        )}
        <p>{segments.map((seg, i) => {
          const placeholder = placeholders[i];
          const answerObj = answers[i] || null;
          const submitted = submittedMap[i] !== undefined;
          return (
            <React.Fragment key={i}>
              <span dangerouslySetInnerHTML={{ __html: seg }} />
              {placeholder && (() => {
                if (placeholder.includes(':MC:')) {
                  return <MultipleChoiceInput placeholder={placeholder} answerObj={answerObj} submitted={submitted} onSubmit={(val)=>handleSubmit(i,val)} />;
                }
                if (placeholder.includes(':NUMERICAL:')) {
                  return <NumericalInput placeholder={placeholder} answerObj={answerObj} submitted={submitted} onSubmit={(val)=>handleSubmit(i,val)} />;
                }
                return <span className="badge bg-secondary">Unsupported</span>;
              })()}
            </React.Fragment>
          );
        })}</p>
      </div>
    </div>
  );
};

const CourseQuestionsList = ({ questions }) => {
  if (!questions || questions.length === 0) return null;
  return (
    <div className="mt-4">
      <h5>Processed XML Questions ({questions.length})</h5>
      {questions.map(q => <CourseQuestion key={q.id} question={q} />)}
    </div>
  );
};

export default CourseQuestionsList;