import React, { useState, useEffect } from 'react';
import { 
  Calendar, 
  Clock, 
  MapPin, 
  BookOpen, 
  AlertTriangle, 
  Plus, 
  Brain, 
  Filter, 
  CheckCircle2, 
  Trash2, 
  Edit3,
  Clock3,
  ShieldCheck,
  Zap
} from 'lucide-react';
import api from '../services/api';

export default function ExamsPage() {
  const [exams, setExams] = useState([]);
  const [summary, setSummary] = useState({ total: 0, upcoming: 0, today: 0, verySoon: 0, within7Days: 0, completed: 0 });
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [showModal, setShowModal] = useState(false);
  const [editingExamId, setEditingExamId] = useState(null);

  // AI Exam Advisor state
  const [isAdvisorLoading, setIsAdvisorLoading] = useState(false);
  const [advisorResult, setAdvisorResult] = useState(null);

  // Form State
  const [title, setTitle] = useState('');
  const [examDate, setExamDate] = useState('');
  const [startTime, setStartTime] = useState('09:00 AM');
  const [location, setLocation] = useState('Hall A2');
  const [syllabus, setSyllabus] = useState('');

  const fetchExams = async (status = filterStatus) => {
    try {
      const res = await api.get(`/exams?status=${status.toLowerCase()}`);
      if (res.data.success) {
        setExams(res.data.data || []);
        if (res.data.summary) {
          setSummary(res.data.summary);
        }
      }
    } catch (err) {
      console.warn('Exams fetch fallback');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchExams(filterStatus);
  }, [filterStatus]);

  const handleRunExamAdvisor = async () => {
    setIsAdvisorLoading(true);
    setAdvisorResult(null);
    try {
      const res = await api.post('/exams/advisor');
      if (res.data.success) {
        setAdvisorResult(res.data.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setIsAdvisorLoading(false);
    }
  };

  const handleOpenAddModal = () => {
    setEditingExamId(null);
    setTitle('');
    setExamDate('');
    setStartTime('09:00 AM');
    setLocation('Hall A2');
    setSyllabus('');
    setShowModal(true);
  };

  const handleOpenEditModal = (exam) => {
    setEditingExamId(exam.id);
    setTitle(exam.title);
    setExamDate(exam.exam_date || exam.examDate);
    setStartTime(exam.start_time || '09:00 AM');
    setLocation(exam.location || 'Hall A2');
    setSyllabus(exam.syllabus || '');
    setShowModal(true);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title || !examDate) return;

    try {
      if (editingExamId) {
        await api.put(`/exams/${editingExamId}`, {
          title,
          exam_date: examDate,
          start_time: startTime,
          location,
          syllabus
        });
      } else {
        await api.post('/exams', {
          title,
          exam_date: examDate,
          start_time: startTime,
          location,
          syllabus
        });
      }
      setShowModal(false);
      fetchExams(filterStatus);
    } catch (err) {
      console.error(err);
    }
  };

  const handleToggleComplete = async (exam) => {
    try {
      const newStatus = exam.status === 'Completed' || exam.examStatus === 'COMPLETED' ? 'Upcoming' : 'Completed';
      await api.put(`/exams/${exam.id}`, { status: newStatus });
      fetchExams(filterStatus);
    } catch (err) {
      console.error(err);
    }
  };

  const handleDeleteExam = async (id) => {
    try {
      await api.delete(`/exams/${id}`);
      fetchExams(filterStatus);
    } catch (err) {
      console.error(err);
    }
  };

  const getUrgencyBadge = (urgency) => {
    switch (urgency) {
      case 'CRITICAL':
        return <span className="badge badge-rose" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><AlertTriangle style={{ width: '12px', height: '12px' }} /> CRITICAL</span>;
      case 'HIGH':
        return <span className="badge badge-amber" style={{ display: 'flex', alignItems: 'center', gap: '4px' }}><Clock3 style={{ width: '12px', height: '12px' }} /> HIGH</span>;
      case 'MEDIUM':
        return <span className="badge badge-indigo">MEDIUM</span>;
      case 'LOW':
        return <span className="badge badge-cyan">LOW</span>;
      case 'COMPLETED':
        return <span className="badge badge-emerald">COMPLETED</span>;
      default:
        return <span className="badge badge-indigo">{urgency}</span>;
    }
  };

  const getDaysBadge = (daysRemaining, examStatus) => {
    if (examStatus === 'COMPLETED') {
      return <span style={{ color: '#10b981', fontWeight: 600, fontSize: '0.8rem' }}>✓ Completed</span>;
    }
    if (examStatus === 'TODAY') {
      return <span style={{ color: '#f43f5e', fontWeight: 800, fontSize: '0.82rem' }}>🚨 EXAM TODAY!</span>;
    }
    if (examStatus === 'VERY_SOON') {
      return <span style={{ color: '#f59e0b', fontWeight: 700, fontSize: '0.82rem' }}>⏳ In {daysRemaining} day(s)</span>;
    }
    return <span style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>📅 In {daysRemaining} days</span>;
  };

  if (loading) return <div className="glass-card" style={{ padding: '60px', textAlign: 'center' }}>⏳ Loading Exam Planner & Intelligence...</div>;

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Header bar */}
      <div className="glass-card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Calendar style={{ width: '22px', height: '22px', color: '#06b6d4' }} /> Exam Planner & Exam Intelligence
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '2px' }}>
            Deterministic exam timing, days remaining, workload integration, and AI Exam Advisory.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button className="btn btn-secondary" onClick={handleRunExamAdvisor} disabled={isAdvisorLoading}>
            <Brain style={{ width: '16px', height: '16px', color: '#06b6d4' }} /> {isAdvisorLoading ? 'Analyzing...' : 'AI Exam Advisor'}
          </button>
          <button className="btn btn-primary" onClick={handleOpenAddModal}>
            <Plus style={{ width: '16px', height: '16px' }} /> Schedule Exam
          </button>
        </div>
      </div>

      {/* Summary Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '14px' }}>
        <div className="glass-card" style={{ padding: '16px', textAlign: 'center' }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>TOTAL</span>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#e2e8f0', marginTop: '4px' }}>{summary.total || exams.length}</div>
        </div>
        <div className="glass-card" style={{ padding: '16px', textAlign: 'center' }}>
          <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600 }}>UPCOMING</span>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#38bdf8', marginTop: '4px' }}>{summary.upcoming || 0}</div>
        </div>
        <div className="glass-card" style={{ padding: '16px', textAlign: 'center' }}>
          <span style={{ fontSize: '0.75rem', color: '#fca5a5', fontWeight: 600 }}>EXAMS TODAY</span>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#f43f5e', marginTop: '4px' }}>{summary.today || 0}</div>
        </div>
        <div className="glass-card" style={{ padding: '16px', textAlign: 'center' }}>
          <span style={{ fontSize: '0.75rem', color: '#fde047', fontWeight: 600 }}>VERY SOON (&le;3d)</span>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#f59e0b', marginTop: '4px' }}>{summary.verySoon || 0}</div>
        </div>
        <div className="glass-card" style={{ padding: '16px', textAlign: 'center' }}>
          <span style={{ fontSize: '0.75rem', color: '#c7d2fe', fontWeight: 600 }}>WITHIN 7 DAYS</span>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#818cf8', marginTop: '4px' }}>{summary.within7Days || 0}</div>
        </div>
        <div className="glass-card" style={{ padding: '16px', textAlign: 'center' }}>
          <span style={{ fontSize: '0.75rem', color: '#6ee7b7', fontWeight: 600 }}>COMPLETED</span>
          <div style={{ fontSize: '1.5rem', fontWeight: 800, color: '#10b981', marginTop: '4px' }}>{summary.completed || 0}</div>
        </div>
      </div>

      {/* AI Exam Advisor Banner Result */}
      {advisorResult && (
        <div className="glass-card" style={{ padding: '20px', background: 'linear-gradient(135deg, rgba(8, 51, 68, 0.9), rgba(15, 23, 42, 0.95))', border: '1px solid rgba(6, 182, 212, 0.3)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#67e8f9', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Brain style={{ width: '18px', height: '18px' }} /> Gemini AI Exam Advisory Analysis
            </h3>
            {advisorResult.isFallback && <span className="badge badge-amber">Deterministic Strategy</span>}
          </div>
          <p style={{ fontSize: '0.9rem', color: '#e2e8f0', lineHeight: '1.5', whiteSpace: 'pre-line' }}>
            {advisorResult.recommendation}
          </p>
        </div>
      )}

      {/* Filter Control Bar */}
      <div className="glass-card" style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
        <Filter style={{ width: '16px', height: '16px', color: 'var(--text-muted)' }} />
        <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600 }}>Filter Exams:</span>
        {['ALL', 'UPCOMING', 'TODAY', 'VERY SOON', 'COMPLETED'].map(statusOpt => (
          <button
            key={statusOpt}
            className={`btn btn-sm ${filterStatus === statusOpt ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setFilterStatus(statusOpt)}
          >
            {statusOpt}
          </button>
        ))}
      </div>

      {/* Exams Grid */}
      {exams.length === 0 ? (
        <div className="glass-card" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
          No exams found for this filter.
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '16px' }}>
          {exams.map((item) => (
            <div key={item.id} className="glass-card glass-card-interactive" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px', border: item.urgency === 'CRITICAL' ? '1px solid rgba(244, 63, 94, 0.4)' : '1px solid var(--border-color)', opacity: item.examStatus === 'COMPLETED' || item.status === 'Completed' ? 0.65 : 1 }}>
              
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <button 
                    onClick={() => handleToggleComplete(item)}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', color: item.examStatus === 'COMPLETED' || item.status === 'Completed' ? '#10b981' : 'var(--text-dim)' }}
                    title={item.status === 'Completed' ? 'Mark as Upcoming' : 'Mark as Completed'}
                  >
                    <CheckCircle2 style={{ width: '20px', height: '20px' }} />
                  </button>
                  <span className="badge badge-cyan">{item.subjectCode || 'CS'}</span>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  {getUrgencyBadge(item.urgency)}
                </div>
              </div>

              <h3 style={{ fontSize: '1.1rem', fontWeight: 700, textDecoration: item.status === 'Completed' || item.examStatus === 'COMPLETED' ? 'line-through' : 'none' }}>
                {item.title}
              </h3>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', fontSize: '0.86rem', color: 'var(--text-muted)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <Calendar style={{ width: '14px', height: '14px', color: '#67e8f9' }} /> Date: <strong style={{ color: '#fff' }}>{item.exam_date || item.examDate}</strong>
                  </div>
                  {getDaysBadge(item.daysRemaining, item.examStatus)}
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Clock style={{ width: '14px', height: '14px', color: '#fde047' }} /> Time: <strong style={{ color: '#fff' }}>{item.start_time || item.startTime || '09:00 AM'}</strong>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <MapPin style={{ width: '14px', height: '14px', color: '#fca5a5' }} /> Location: <strong style={{ color: '#fff' }}>{item.location || 'Main Hall'}</strong>
                </div>
              </div>

              {item.syllabus && (
                <div style={{ background: 'rgba(255, 255, 255, 0.03)', padding: '10px 12px', borderRadius: '6px', border: '1px solid var(--border-color)', marginTop: '4px' }}>
                  <div style={{ fontSize: '0.78rem', color: '#c7d2fe', fontWeight: 700, marginBottom: '2px' }}>Syllabus Scope:</div>
                  <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)' }}>{item.syllabus}</p>
                </div>
              )}

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '6px', borderTop: '1px solid var(--border-color)', paddingTop: '10px' }}>
                <button className="btn btn-secondary btn-sm" onClick={() => handleOpenEditModal(item)}>
                  <Edit3 style={{ width: '14px', height: '14px' }} /> Edit
                </button>
                <button className="btn btn-secondary btn-sm" onClick={() => handleDeleteExam(item.id)} style={{ color: '#fca5a5' }}>
                  <Trash2 style={{ width: '14px', height: '14px' }} /> Delete
                </button>
              </div>

            </div>
          ))}
        </div>
      )}

      {/* Schedule / Edit Modal */}
      {showModal && (
        <div style={{
          position: 'fixed',
          top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0, 0, 0, 0.75)',
          backdropFilter: 'blur(8px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 100
        }}>
          <div className="glass-card" style={{ width: '100%', maxWidth: '500px', padding: '28px' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, marginBottom: '16px' }}>
              {editingExamId ? 'Edit Exam' : 'Schedule New Exam'}
            </h3>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Exam Title</label>
                <input type="text" className="input-field" required placeholder="e.g. DSA Midterm Exam" value={title} onChange={(e) => setTitle(e.target.value)} />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Exam Date</label>
                  <input type="date" className="input-field" required value={examDate} onChange={(e) => setExamDate(e.target.value)} />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Start Time</label>
                  <input type="text" className="input-field" placeholder="e.g. 09:00 AM" value={startTime} onChange={(e) => setStartTime(e.target.value)} />
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Hall / Location</label>
                <input type="text" className="input-field" placeholder="e.g. Hall A2" value={location} onChange={(e) => setLocation(e.target.value)} />
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Syllabus Scope</label>
                <textarea className="input-field" rows="3" placeholder="Topics covered in exam..." value={syllabus} onChange={(e) => setSyllabus(e.target.value)} />
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '12px' }}>
                <button type="button" className="btn btn-secondary" style={{ width: '50%' }} onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" style={{ width: '50%' }}>
                  {editingExamId ? 'Update Exam' : 'Save Exam'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
