import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getCourseById, evaluateFlashCardAnswer } from '../../services/courseService';
import { FaArrowLeft, FaSync, FaRandom } from 'react-icons/fa';
import '../FlashCards.css'; // Make sure this path is correct

const FlashCardsList = () => {
  const { id } = useParams();
  const [course, setCourse] = useState(null);
  const [flashCards, setFlashCards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [flippedCards, setFlippedCards] = useState({});
  const [answers, setAnswers] = useState({});
  const [evaluations, setEvaluations] = useState({});
  const [submitting, setSubmitting] = useState({});

  useEffect(() => {
    const fetchCourse = async () => {
      try {
        const response = await getCourseById(id);
        setCourse(response.data);
        
        // Initialize flash cards with additional properties
        if (response.data.flashCards && response.data.flashCards.length > 0) {
          setFlashCards(response.data.flashCards.map(card => ({
            ...card,
            id: card.id || Math.random().toString(36).substr(2, 9)
          })));
        }
      } catch (err) {
        setError('Failed to load flash cards');
        console.error('Error fetching course details:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchCourse();
  }, [id]);

  // Simplified toggle function - just flips the card
  const toggleCard = (cardId) => {
    console.log('Toggling card:', cardId);
    setFlippedCards(prev => ({
      ...prev,
      [cardId]: !prev[cardId]
    }));
  };

  const shuffleCards = () => {
    setFlashCards(cards => [...cards].sort(() => Math.random() - 0.5));
  };

  const resetCards = () => {
    setFlippedCards({});
    setAnswers({});
    setEvaluations({});
    setSubmitting({});
    // Re-fetch from course to restore original order
    if (course && course.flashCards) {
      setFlashCards(course.flashCards.map(card => ({
        ...card,
        id: card.id || Math.random().toString(36).substr(2, 9)
      })));
    }
  };

  const onAnswerChange = (cardId, value) => {
    setAnswers(prev => ({ ...prev, [cardId]: value }));
  };

  const submitAnswer = async (card) => {
    setSubmitting(prev => ({ ...prev, [card.id]: true }));
    try {
      const resp = await evaluateFlashCardAnswer(id, {
        question: card.question,
        expectedAnswer: card.answer,
        userAnswer: answers[card.id] || '',
      });
      setEvaluations(prev => ({ ...prev, [card.id]: resp.data }));
    } catch (e) {
      console.error('Evaluation failed', e);
      setEvaluations(prev => ({ ...prev, [card.id]: { error: 'Evaluation failed' } }));
    } finally {
      setSubmitting(prev => ({ ...prev, [card.id]: false }));
    }
  };

  // Loading, error, and empty states (unchanged)
  if (loading) {
    return (
      <div className="container mt-5 text-center">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
        <p className="mt-3">Loading flash cards...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container mt-4">
        <div className="alert alert-danger">{error}</div>
        <Link to={`/courses/${id}`} className="btn btn-primary">
          <FaArrowLeft className="me-2" /> Back to Course
        </Link>
      </div>
    );
  }

  if (!course) {
    return (
      <div className="container mt-4">
        <div className="alert alert-warning">Course not found</div>
        <Link to="/courses" className="btn btn-primary">
          <FaArrowLeft className="me-2" /> Back to Courses
        </Link>
      </div>
    );
  }

  if (!flashCards || flashCards.length === 0) {
    return (
      <div className="container mt-4">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <h2>Flash Cards</h2>
          <Link to={`/courses/${id}`} className="btn btn-outline-primary">
            <FaArrowLeft className="me-2" /> Back to Course
          </Link>
        </div>
        <div className="alert alert-info">
          No flash cards available for this course.
        </div>
      </div>
    );
  }

  // The main render method
  return (
    <div className="container mt-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h2>Flash Cards for {course.title}</h2>
        <div>
          <button 
            onClick={shuffleCards} 
            className="btn btn-outline-secondary me-2"
            title="Shuffle cards"
          >
            <FaRandom className="me-1" /> Shuffle
          </button>
          <button 
            onClick={resetCards} 
            className="btn btn-outline-secondary me-2"
            title="Reset all cards"
          >
            <FaSync className="me-1" /> Reset
          </button>
          <Link to={`/courses/${id}`} className="btn btn-outline-primary">
            <FaArrowLeft className="me-2" /> Back to Course
          </Link>
        </div>
      </div>

      <p className="text-muted mb-4">Click on a card to flip it and see the answer.</p>
      
      <div className="row">
        {flashCards.map((card, index) => (
          <div className="col-md-6 col-lg-4 mb-4" key={card.id}>
            <div className="flash-card-container">
              <div 
                className={`flash-card ${flippedCards[card.id] ? 'flipped' : ''}`} 
                onClick={() => toggleCard(card.id)}
              >
                <div className="card-face card-front">
                  <div className="card-counter">{index + 1}</div>
                  <h5 className="card-title">Question</h5>
                  <div className="card-content">{card.question}</div>
                  <div className="card-hint">Click to see answer</div>
                </div>
                <div className="card-face card-back">
                  <div className="card-counter">{index + 1}</div>
                  <h5 className="card-title">Answer</h5>
                  <div className="card-content">{card.answer}</div>
                  <div className="card-hint">Click to see question</div>
                </div>
              </div>
              <div className="flash-card-eval">
                <textarea
                  placeholder="Type your answer, then press Check"
                  value={answers[card.id] || ''}
                  onChange={(e) => onAnswerChange(card.id, e.target.value)}
                  rows={3}
                />
                <div className="eval-actions">
                  <button
                    className="btn btn-sm btn-primary"
                    onClick={() => submitAnswer(card)}
                    disabled={submitting[card.id]}
                  >
                    {submitting[card.id] ? 'Checking…' : 'Check Answer'}
                  </button>
                  {evaluations[card.id]?.evaluation && (
                    <span className={`verdict-badge fade-in ${
                      (evaluations[card.id].evaluation.verdict || '').toLowerCase() === 'correct' ? 'verdict-correct' :
                      (evaluations[card.id].evaluation.verdict || '').toLowerCase() === 'partially correct' ? 'verdict-partially' :
                      (evaluations[card.id].evaluation.verdict || '').toLowerCase() === 'incorrect' ? 'verdict-incorrect' : ''
                    }`}
                    >
                      {evaluations[card.id].evaluation.verdict || 'Evaluated'}
                      {typeof evaluations[card.id].evaluation.score !== 'undefined' ? ` • ${evaluations[card.id].evaluation.score}` : ''}
                    </span>
                  )}
                </div>
                {evaluations[card.id]?.evaluation?.feedback && (
                  <div className="feedback-box fade-in">
                    <strong>Feedback:</strong> {evaluations[card.id].evaluation.feedback}
                  </div>
                )}
                {evaluations[card.id]?.sourceFileName && (
                  <div className="source-file fade-in">
                    {evaluations[card.id].sourceFileName}
                  </div>
                )}
                {evaluations[card.id]?.error && (
                  <div className="error-msg fade-in">{evaluations[card.id].error}</div>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default FlashCardsList;