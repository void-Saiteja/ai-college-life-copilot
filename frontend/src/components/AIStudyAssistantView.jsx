import React, { useState } from 'react';
import api from '../services/api';
import { 
  BrainCircuit, 
  Sparkles, 
  BookOpen, 
  MessageSquare, 
  HelpCircle, 
  CheckCircle2, 
  XCircle, 
  Send, 
  RotateCw,
  Award
} from 'lucide-react';
import confetti from 'canvas-confetti';

export default function AIStudyAssistantView({ notes = [], apiBase }) {
  const [subTab, setSubTab] = useState('summarizer'); // 'summarizer', 'flashcards', 'quiz', 'chat'
  
  // Note Summarizer state
  const [noteTitle, setNoteTitle] = useState('');
  const [courseName, setCourseName] = useState('');
  const [rawContent, setRawContent] = useState('');
  const [isSummarizing, setIsSummarizing] = useState(false);
  const [aiResult, setAiResult] = useState(null);

  // Flashcards state
  const [cardIndex, setCardIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);

  // Quiz state
  const [selectedAnswers, setSelectedAnswers] = useState({});
  const [quizScore, setQuizScore] = useState(null);

  // Chat state
  const [messages, setMessages] = useState([
    { sender: 'ai', text: '👋 Hi! I am your AI Study Copilot. Ask me anything about your course concepts, study techniques, or exam prep strategies!' }
  ]);
  const [chatInput, setChatInput] = useState('');
  const [isChatting, setIsChatting] = useState(false);

  // Trigger Summarize API
  const handleSummarize = async () => {
    if (!rawContent.trim()) return;
    setIsSummarizing(true);
    try {
      const res = await api.post('/ai/summarize', {
        title: noteTitle,
        course: courseName,
        content: rawContent
      });
      if (res.data.success) {
        setAiResult(res.data.data);
        setCardIndex(0);
        setIsFlipped(false);
        setSelectedAnswers({});
        setQuizScore(null);
      }
    } catch (err) {
      console.error('AI Summarization failed', err);
    } finally {
      setIsSummarizing(false);
    }
  };

  // Submit Chat
  const handleSendChat = async (e) => {
    e.preventDefault();
    if (!chatInput.trim()) return;

    const userMsg = chatInput;
    setMessages(prev => [...prev, { sender: 'user', text: userMsg }]);
    setChatInput('');
    setIsChatting(true);

    try {
      const res = await api.post('/ai/chat', { message: userMsg });
      if (res.data.success) {
        setMessages(prev => [...prev, { sender: 'ai', text: res.data.reply }]);
      }
    } catch (err) {
      setMessages(prev => [...prev, { sender: 'ai', text: 'Sorry, I encountered an issue connecting to AI backend.' }]);
    } finally {
      setIsChatting(false);
    }
  };

  // Submit Quiz
  const handleQuizSubmit = () => {
    if (!aiResult?.quiz) return;
    let correct = 0;
    aiResult.quiz.forEach((q) => {
      if (selectedAnswers[q.id] === q.correctAnswer) {
        correct++;
      }
    });
    const scorePct = Math.round((correct / aiResult.quiz.length) * 100);
    setQuizScore(scorePct);

    if (scorePct >= 80) {
      confetti({ particleCount: 100, spread: 70, origin: { y: 0.6 } });
    }
  };

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Header */}
      <div className="glass-card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <BrainCircuit style={{ width: '22px', height: '22px', color: '#818cf8' }} /> AI Study Intelligence Assistant
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '2px' }}>
            Transform raw lecture notes into active recall flashcards, practice quizzes, & AI insights.
          </p>
        </div>

        {/* Subtab Toggle Buttons */}
        <div style={{ display: 'flex', gap: '8px', background: 'rgba(15, 23, 42, 0.6)', padding: '4px', borderRadius: '12px', border: '1px solid var(--border-color)' }}>
          <button 
            className={`btn btn-sm ${subTab === 'summarizer' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setSubTab('summarizer')}
          >
            <BookOpen style={{ width: '14px', height: '14px' }} /> Note Summarizer
          </button>
          <button 
            className={`btn btn-sm ${subTab === 'flashcards' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setSubTab('flashcards')}
            disabled={!aiResult}
          >
            <RotateCw style={{ width: '14px', height: '14px' }} /> Flashcards
          </button>
          <button 
            className={`btn btn-sm ${subTab === 'quiz' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setSubTab('quiz')}
            disabled={!aiResult}
          >
            <HelpCircle style={{ width: '14px', height: '14px' }} /> Practice Quiz
          </button>
          <button 
            className={`btn btn-sm ${subTab === 'chat' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setSubTab('chat')}
          >
            <MessageSquare style={{ width: '14px', height: '14px' }} /> Copilot Chat
          </button>
        </div>
      </div>

      {/* SubTab 1: Note Summarizer */}
      {subTab === 'summarizer' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: '20px' }}>
          
          {/* Input Panel */}
          <div className="glass-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>Paste Lecture / Textbook Notes</h3>
              {notes.length > 0 && (
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Load Saved Note:</span>
                  <select 
                    className="input-field" 
                    style={{ padding: '4px 10px', fontSize: '0.8rem', width: 'auto' }}
                    defaultValue=""
                    onChange={(e) => {
                      const found = notes.find(n => n.id === e.target.value);
                      if (found) {
                        setNoteTitle(found.title);
                        setCourseName(found.course);
                        setRawContent(found.content);
                      }
                    }}
                  >
                    <option value="" disabled>-- Select a Note --</option>
                    {notes.map(n => (
                      <option key={n.id} value={n.id}>{n.course}: {n.title}</option>
                    ))}
                  </select>
                </div>
              )}
            </div>
            
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Topic / Note Title</label>
                <input 
                  type="text" 
                  className="input-field" 
                  placeholder="e.g. Data Structures - BSTs" 
                  value={noteTitle}
                  onChange={(e) => setNoteTitle(e.target.value)}
                />
              </div>
              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Course Code</label>
                <input 
                  type="text" 
                  className="input-field" 
                  placeholder="e.g. CS 201" 
                  value={courseName}
                  onChange={(e) => setCourseName(e.target.value)}
                />
              </div>
            </div>

            <div>
              <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Raw Study Text Content</label>
              <textarea 
                className="input-field textarea-field" 
                rows={8}
                placeholder="Paste your raw lecture notes, syllabus concepts, or textbook extracts here..." 
                value={rawContent}
                onChange={(e) => setRawContent(e.target.value)}
              />
            </div>

            <button 
              className="btn btn-primary btn-lg" 
              onClick={handleSummarize}
              disabled={isSummarizing || !rawContent.trim()}
              style={{ width: '100%', marginTop: '8px' }}
            >
              {isSummarizing ? (
                <>Analyzing with AI...</>
              ) : (
                <>
                  <Sparkles style={{ width: '18px', height: '18px' }} /> Generate AI Summary & Flashcards
                </>
              )}
            </button>
          </div>

          {/* Results Panel */}
          <div className="glass-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700 }}>AI Generated Study Breakdown</h3>

            {!aiResult ? (
              <div style={{ padding: '60px 20px', textAlign: 'center', color: 'var(--text-muted)' }}>
                <BrainCircuit style={{ width: '48px', height: '48px', color: '#475569', marginBottom: '12px' }} />
                <p>Paste text on the left and click "Generate AI Summary" to extract flashcards, key takeaways, and practice quiz.</p>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                
                {/* Stats Pill */}
                <div style={{ display: 'flex', gap: '12px' }}>
                  <span className="badge badge-indigo">
                    📖 {aiResult.wordCount} words
                  </span>
                  <span className="badge badge-cyan">
                    ⏱️ ~{aiResult.readingTimeMinutes} min read
                  </span>
                </div>

                {/* Summary */}
                <div style={{ background: 'rgba(99, 102, 241, 0.08)', padding: '14px', borderRadius: 'var(--radius-sm)', border: '1px solid rgba(99, 102, 241, 0.2)' }}>
                  <h4 style={{ fontSize: '0.9rem', color: '#a5b4fc', marginBottom: '4px' }}>Overview Summary</h4>
                  <p style={{ fontSize: '0.9rem', lineHeight: '1.5' }}>{aiResult.summary}</p>
                </div>

                {/* Key Takeaways */}
                <div>
                  <h4 style={{ fontSize: '0.9rem', fontWeight: 700, marginBottom: '8px', color: '#67e8f9' }}>Key Takeaways</h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    {aiResult.keyTakeaways.map((takeaway, idx) => (
                      <div key={idx} style={{ fontSize: '0.86rem', background: 'rgba(255, 255, 255, 0.03)', padding: '8px 12px', borderRadius: '6px' }}>
                        {takeaway}
                      </div>
                    ))}
                  </div>
                </div>

                {/* Quick actions */}
                <div style={{ display: 'flex', gap: '10px', marginTop: '10px' }}>
                  <button className="btn btn-secondary btn-sm" onClick={() => setSubTab('flashcards')}>
                    Practice Flashcards ({aiResult.flashcards.length})
                  </button>
                  <button className="btn btn-emerald btn-sm" onClick={() => setSubTab('quiz')}>
                    Take Practice Quiz ({aiResult.quiz.length})
                  </button>
                </div>

              </div>
            )}
          </div>

        </div>
      )}

      {/* SubTab 2: Flashcards */}
      {subTab === 'flashcards' && aiResult && (
        <div className="glass-card" style={{ padding: '40px', maxWidth: '650px', margin: '0 auto', width: '100%', textAlign: 'center' }}>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '16px' }}>
            Flashcard {cardIndex + 1} of {aiResult.flashcards.length}
          </div>

          <div 
            onClick={() => setIsFlipped(!isFlipped)}
            style={{
              minHeight: '220px',
              padding: '30px',
              borderRadius: 'var(--radius-md)',
              background: isFlipped ? 'linear-gradient(135deg, rgba(30, 27, 75, 0.9), rgba(15, 23, 42, 0.95))' : 'linear-gradient(135deg, rgba(99, 102, 241, 0.15), rgba(6, 182, 212, 0.15))',
              border: isFlipped ? '2px solid #818cf8' : '2px dashed rgba(99, 102, 241, 0.4)',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              alignItems: 'center',
              cursor: 'pointer',
              transition: 'all 0.3s ease',
              marginBottom: '24px'
            }}
          >
            <span style={{ fontSize: '0.78rem', color: isFlipped ? '#a5b4fc' : '#67e8f9', textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: '8px' }}>
              {isFlipped ? 'Answer (Click to Flip)' : 'Question (Click to Reveal)'}
            </span>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, lineHeight: '1.4' }}>
              {isFlipped ? aiResult.flashcards[cardIndex].answer : aiResult.flashcards[cardIndex].question}
            </h3>
          </div>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '16px' }}>
            <button 
              className="btn btn-secondary" 
              onClick={() => {
                setCardIndex((prev) => Math.max(0, prev - 1));
                setIsFlipped(false);
              }}
              disabled={cardIndex === 0}
            >
              Previous
            </button>
            <button 
              className="btn btn-primary" 
              onClick={() => {
                setCardIndex((prev) => Math.min(aiResult.flashcards.length - 1, prev + 1));
                setIsFlipped(false);
              }}
              disabled={cardIndex === aiResult.flashcards.length - 1}
            >
              Next Flashcard
            </button>
          </div>
        </div>
      )}

      {/* SubTab 3: Practice Quiz */}
      {subTab === 'quiz' && aiResult && (
        <div className="glass-card" style={{ padding: '28px', maxWidth: '750px', margin: '0 auto', width: '100%' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800 }}>Practice Quiz Assessment</h3>
            {quizScore !== null && (
              <span className={`badge ${quizScore >= 80 ? 'badge-emerald' : 'badge-amber'}`} style={{ padding: '6px 14px', fontSize: '0.9rem' }}>
                <Award style={{ width: '16px', height: '16px' }} /> Score: {quizScore}%
              </span>
            )}
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
            {aiResult.quiz.map((q, qIdx) => (
              <div key={q.id} style={{ padding: '18px', background: 'rgba(255, 255, 255, 0.03)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-color)' }}>
                <h4 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: '12px' }}>
                  {qIdx + 1}. {q.question}
                </h4>

                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {q.options.map((opt, optIdx) => {
                    const isSelected = selectedAnswers[q.id] === optIdx;
                    return (
                      <button
                        key={optIdx}
                        onClick={() => setSelectedAnswers(prev => ({ ...prev, [q.id]: optIdx }))}
                        style={{
                          textAlign: 'left',
                          padding: '10px 14px',
                          borderRadius: '8px',
                          border: isSelected ? '2px solid #6366f1' : '1px solid var(--border-color)',
                          background: isSelected ? 'rgba(99, 102, 241, 0.15)' : 'rgba(15, 23, 42, 0.4)',
                          color: '#fff',
                          cursor: 'pointer',
                          fontSize: '0.9rem'
                        }}
                      >
                        {String.fromCharCode(65 + optIdx)}. {opt}
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}

            <button className="btn btn-emerald btn-lg" onClick={handleQuizSubmit} style={{ width: '100%' }}>
              Submit Quiz Answers & Get Feedback
            </button>
          </div>
        </div>
      )}

      {/* SubTab 4: Copilot Chat */}
      {subTab === 'chat' && (
        <div className="glass-card" style={{ padding: '24px', maxWidth: '850px', margin: '0 auto', width: '100%', height: '550px', display: 'flex', flexDirection: 'column' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '14px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <MessageSquare style={{ width: '18px', height: '18px', color: '#818cf8' }} /> Student Copilot AI Companion
          </h3>

          {/* Messages Window */}
          <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '12px', paddingRight: '6px', marginBottom: '16px' }}>
            {messages.map((msg, idx) => (
              <div 
                key={idx}
                style={{
                  alignSelf: msg.sender === 'user' ? 'flex-end' : 'flex-start',
                  maxWidth: '80%',
                  padding: '12px 16px',
                  borderRadius: msg.sender === 'user' ? '16px 16px 2px 16px' : '16px 16px 16px 2px',
                  background: msg.sender === 'user' ? 'linear-gradient(135deg, #6366f1, #4f46e5)' : 'rgba(255, 255, 255, 0.06)',
                  border: msg.sender === 'user' ? 'none' : '1px solid var(--border-color)',
                  color: '#fff',
                  fontSize: '0.92rem',
                  lineHeight: '1.5'
                }}
              >
                {msg.text}
              </div>
            ))}
          </div>

          {/* Input form */}
          <form onSubmit={handleSendChat} style={{ display: 'flex', gap: '10px' }}>
            <input 
              type="text" 
              className="input-field"
              placeholder="Ask Copilot for study tips, exam strategies, or stress management..." 
              value={chatInput}
              onChange={(e) => setChatInput(e.target.value)}
              disabled={isChatting}
            />
            <button type="submit" className="btn btn-primary" disabled={isChatting || !chatInput.trim()}>
              <Send style={{ width: '16px', height: '16px' }} />
            </button>
          </form>
        </div>
      )}

    </div>
  );
}
