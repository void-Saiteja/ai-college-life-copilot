import React, { useState, useEffect } from 'react';
import { 
  HelpCircle, 
  Sparkles, 
  Award, 
  CheckCircle2, 
  XCircle,
  RotateCw, 
  BookOpen, 
  Brain, 
  Clock, 
  ChevronRight, 
  ChevronLeft,
  AlertTriangle,
  History,
  TrendingDown
} from 'lucide-react';
import confetti from 'canvas-confetti';
import api from '../services/api';

export default function AIQuizPage() {
  const [subject, setSubject] = useState('CS 201');
  const [topic, setTopic] = useState('Data Structures & Algorithms');
  const [difficulty, setDifficulty] = useState('Medium');
  const [questionCount, setQuestionCount] = useState(5);
  
  const [quizData, setQuizData] = useState(null);
  const [currentQIdx, setCurrentQIdx] = useState(0);
  const [loading, setLoading] = useState(false);
  const [selectedAnswers, setSelectedAnswers] = useState({});
  const [quizResult, setQuizResult] = useState(null);

  // History & Advisor state
  const [history, setHistory] = useState([]);
  const [advisorResult, setAdvisorResult] = useState(null);
  const [isAdvisorLoading, setIsAdvisorLoading] = useState(false);

  const fetchHistory = async () => {
    try {
      const res = await api.get('/quizzes/history');
      if (res.data.success) {
        setHistory(res.data.data.attempts || res.data.data || []);
      }
    } catch (err) {
      console.warn('Quiz history fetch fallback');
    }
  };

  useEffect(() => {
    fetchHistory();
  }, []);

  const handleGenerateQuiz = async (e) => {
    e.preventDefault();
    setLoading(true);
    setQuizResult(null);
    setSelectedAnswers({});
    setCurrentQIdx(0);

    try {
      const res = await api.post('/quizzes/generate', { subject, topic, difficulty, questionCount: Number(questionCount) });
      if (res.data.success) {
        setQuizData(res.data.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmitQuiz = async () => {
    if (!quizData) return;
    try {
      const res = await api.post(`/quizzes/${quizData.quizId || 'submit'}/submit`, {
        subject: quizData.subject,
        topic: quizData.topic,
        answers: selectedAnswers,
        rawQuestions: quizData._rawAnswers || []
      });

      if (res.data.success) {
        setQuizResult(res.data.data);
        if (res.data.data.percentage >= 75) {
          confetti({ particleCount: 120, spread: 80, origin: { y: 0.6 } });
        }
        fetchHistory();
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleRunAdvisor = async () => {
    setIsAdvisorLoading(true);
    setAdvisorResult(null);
    try {
      const res = await api.post('/quizzes/advisor');
      if (res.data.success) {
        setAdvisorResult(res.data.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsAdvisorLoading(false);
    }
  };

  // Weak topics calculation from history
  const weakTopics = Array.from(new Set(
    history.filter(h => h.percentage < 60).map(h => `${h.subject || 'CS'} - ${h.topic}`)
  ));

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Header bar */}
      <div className="glass-card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <HelpCircle style={{ width: '22px', height: '22px', color: '#fde047' }} /> AI MCQ Quiz Generator & Revision Engine
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '2px' }}>
            Generate subject quizzes, track weak topics, and evaluate exam readiness.
          </p>
        </div>

        <button className="btn btn-secondary" onClick={handleRunAdvisor} disabled={isAdvisorLoading}>
          <Brain style={{ width: '16px', height: '16px', color: '#fde047' }} /> {isAdvisorLoading ? 'Analyzing...' : 'AI Quiz Advisor'}
        </button>
      </div>

      {/* Weak Topics & History Summary Banner */}
      {weakTopics.length > 0 && !quizData && (
        <div className="glass-card" style={{ padding: '16px 20px', background: 'rgba(244, 63, 94, 0.08)', border: '1px solid rgba(244, 63, 94, 0.2)' }}>
          <h4 style={{ fontSize: '0.9rem', fontWeight: 700, color: '#fca5a5', display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
            <TrendingDown style={{ width: '16px', height: '16px' }} /> Weak Topics Identified (&lt;60% Avg Score):
          </h4>
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            {weakTopics.map((wt, i) => (
              <span key={i} className="badge badge-rose">⚠️ {wt}</span>
            ))}
          </div>
        </div>
      )}

      {/* AI Quiz Advisor Result Panel */}
      {advisorResult && (
        <div className="glass-card" style={{ padding: '20px', background: 'linear-gradient(135deg, rgba(30, 27, 75, 0.9), rgba(15, 23, 42, 0.95))', border: '1px solid rgba(253, 224, 71, 0.3)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#fde047', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Brain style={{ width: '18px', height: '18px' }} /> Gemini AI Quiz Revision Advisory
            </h3>
            {advisorResult.isFallback && <span className="badge badge-amber">Deterministic Advisory</span>}
          </div>
          <p style={{ fontSize: '0.9rem', color: '#e2e8f0', lineHeight: '1.5', whiteSpace: 'pre-line' }}>
            {advisorResult.recommendation}
          </p>
        </div>
      )}

      {/* Quiz Setup Form */}
      {!quizData && (
        <div className="glass-card" style={{ padding: '28px', maxWidth: '650px', margin: '0 auto', width: '100%' }}>
          <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '16px' }}>Configure AI Quiz Test</h3>

          <form onSubmit={handleGenerateQuiz} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            <div>
              <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Subject</label>
              <input type="text" className="input-field" required value={subject} onChange={e=>setSubject(e.target.value)} placeholder="e.g. CS 201" />
            </div>

            <div>
              <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Topic / Unit Name</label>
              <input type="text" className="input-field" required value={topic} onChange={e=>setTopic(e.target.value)} placeholder="e.g. Binary Search Trees & AVL Rotations" />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Difficulty Level</label>
                <select className="input-field" value={difficulty} onChange={e=>setDifficulty(e.target.value)}>
                  <option value="Easy">Easy</option>
                  <option value="Medium">Medium</option>
                  <option value="Hard">Hard</option>
                </select>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Number of Questions</label>
                <select className="input-field" value={questionCount} onChange={e=>setQuestionCount(Number(e.target.value))}>
                  <option value={3}>3 Questions</option>
                  <option value={5}>5 Questions</option>
                  <option value={10}>10 Questions</option>
                </select>
              </div>
            </div>

            <button type="submit" className="btn btn-primary btn-lg" disabled={loading} style={{ width: '100%', marginTop: '8px' }}>
              {loading ? 'Generating AI Quiz Questions...' : '🚀 Generate AI Quiz'}
            </button>
          </form>
        </div>
      )}

      {/* Active Quiz Test View */}
      {quizData && !quizResult && (
        <div className="glass-card" style={{ padding: '28px', maxWidth: '750px', margin: '0 auto', width: '100%' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <div>
              <span className="badge badge-indigo">{quizData.subject}</span>
              <h3 style={{ fontSize: '1.2rem', fontWeight: 800, marginTop: '4px' }}>{quizData.topic} Quiz</h3>
            </div>
            <button className="btn btn-secondary btn-sm" onClick={() => setQuizData(null)}>
              Cancel
            </button>
          </div>

          {/* Progress bar */}
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '8px' }}>
            <span>Question {currentQIdx + 1} of {quizData.questions.length}</span>
            <span>Difficulty: {quizData.difficulty}</span>
          </div>

          {quizData.questions[currentQIdx] && (
            <div style={{ padding: '20px', borderRadius: '10px', background: 'rgba(255, 255, 255, 0.03)', border: '1px solid var(--border-color)', marginBottom: '20px' }}>
              <div style={{ fontWeight: 700, fontSize: '1.05rem', marginBottom: '16px', lineHeight: '1.5' }}>
                {currentQIdx + 1}. {quizData.questions[currentQIdx].question}
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                {quizData.questions[currentQIdx].options.map((opt, optIdx) => {
                  const isSel = selectedAnswers[quizData.questions[currentQIdx].id] === optIdx;
                  return (
                    <button
                      key={optIdx}
                      onClick={() => setSelectedAnswers(prev => ({ ...prev, [quizData.questions[currentQIdx].id]: optIdx }))}
                      style={{
                        textAlign: 'left',
                        padding: '12px 16px',
                        borderRadius: '8px',
                        border: isSel ? '2px solid #6366f1' : '1px solid var(--border-color)',
                        background: isSel ? 'rgba(99, 102, 241, 0.2)' : 'rgba(15, 23, 42, 0.6)',
                        color: '#fff',
                        cursor: 'pointer',
                        fontSize: '0.92rem',
                        transition: 'all 0.2s ease'
                      }}
                    >
                      <strong style={{ marginRight: '8px', color: isSel ? '#a5b4fc' : 'var(--text-muted)' }}>
                        {String.fromCharCode(65 + optIdx)}.
                      </strong> 
                      {opt}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Navigation Controls */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <button 
              className="btn btn-secondary" 
              onClick={() => setCurrentQIdx(prev => Math.max(0, prev - 1))}
              disabled={currentQIdx === 0}
            >
              <ChevronLeft style={{ width: '16px', height: '16px' }} /> Previous
            </button>

            {currentQIdx < quizData.questions.length - 1 ? (
              <button 
                className="btn btn-primary" 
                onClick={() => setCurrentQIdx(prev => Math.min(quizData.questions.length - 1, prev + 1))}
              >
                Next <ChevronRight style={{ width: '16px', height: '16px' }} />
              </button>
            ) : (
              <button className="btn btn-emerald" onClick={handleSubmitQuiz}>
                Submit Answers & Evaluate
              </button>
            )}
          </div>
        </div>
      )}

      {/* Quiz Evaluation Result & Full Review */}
      {quizResult && (
        <div className="glass-card animate-fade-in" style={{ padding: '32px', maxWidth: '750px', margin: '0 auto', width: '100%' }}>
          
          <div style={{ textAlign: 'center', marginBottom: '24px' }}>
            <Award style={{ width: '48px', height: '48px', color: '#fde047', marginBottom: '8px' }} />
            <h3 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Quiz Assessment Completed</h3>
            <div style={{ fontSize: '2.4rem', fontWeight: 800, color: quizResult.percentage >= 80 ? '#6ee7b7' : quizResult.percentage >= 60 ? '#fde047' : '#fca5a5', marginTop: '6px' }}>
              {quizResult.percentage}% Score ({quizResult.score} / {quizResult.totalQuestions})
            </div>
            
            <div style={{ display: 'flex', justifyContent: 'center', gap: '16px', marginTop: '12px', fontSize: '0.88rem' }}>
              <span style={{ color: '#6ee7b7' }}>✓ Correct: <strong>{quizResult.correctCount ?? quizResult.score}</strong></span>
              <span style={{ color: '#fca5a5' }}>✗ Incorrect: <strong>{quizResult.incorrectCount ?? 0}</strong></span>
              <span style={{ color: 'var(--text-muted)' }}>⚪ Unanswered: <strong>{quizResult.unansweredCount ?? 0}</strong></span>
            </div>

            <p style={{ color: 'var(--text-muted)', fontSize: '0.92rem', marginTop: '12px' }}>{quizResult.aiFeedback}</p>
          </div>

          {/* Full Question Review */}
          {quizResult.results && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginBottom: '24px' }}>
              <h4 style={{ fontSize: '1rem', fontWeight: 700, color: '#a5b4fc', borderBottom: '1px solid var(--border-color)', paddingBottom: '8px' }}>
                Detailed Answer Review & Explanations:
              </h4>

              {quizResult.results.map((r, idx) => (
                <div 
                  key={r.id} 
                  style={{ 
                    padding: '16px', 
                    borderRadius: '8px', 
                    background: r.isCorrect ? 'rgba(16, 185, 129, 0.05)' : 'rgba(244, 63, 94, 0.05)', 
                    border: r.isCorrect ? '1px solid rgba(16, 185, 129, 0.2)' : '1px solid rgba(244, 63, 94, 0.2)' 
                  }}
                >
                  <div style={{ fontWeight: 700, fontSize: '0.95rem', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                    {r.isCorrect ? <CheckCircle2 style={{ width: '18px', height: '18px', color: '#10b981' }} /> : <XCircle style={{ width: '18px', height: '18px', color: '#f43f5e' }} />}
                    <span>{idx + 1}. {r.question}</span>
                  </div>

                  <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', display: 'flex', flexDirection: 'column', gap: '4px', margin: '8px 0' }}>
                    <div>Your Answer: <strong style={{ color: r.isCorrect ? '#6ee7b7' : '#fca5a5' }}>{r.selectedAnswer !== null && r.selectedAnswer !== undefined ? `${String.fromCharCode(65 + r.selectedAnswer)}. ${r.options?.[r.selectedAnswer] || ''}` : 'Unanswered'}</strong></div>
                    {!r.isCorrect && <div>Correct Answer: <strong style={{ color: '#6ee7b7' }}>{String.fromCharCode(65 + r.correctAnswer)}. {r.options?.[r.correctAnswer]}</strong></div>}
                  </div>

                  <div style={{ fontSize: '0.82rem', color: 'var(--text-main)', background: 'rgba(255, 255, 255, 0.04)', padding: '8px 12px', borderRadius: '6px', marginTop: '6px' }}>
                    <strong>Explanation:</strong> {r.explanation}
                  </div>
                </div>
              ))}
            </div>
          )}

          <button className="btn btn-primary btn-lg" onClick={() => { setQuizData(null); setQuizResult(null); }} style={{ width: '100%' }}>
            <RotateCw style={{ width: '18px', height: '18px' }} /> Take Another Quiz
          </button>
        </div>
      )}

    </div>
  );
}
