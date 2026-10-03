import React, { useState } from 'react';
import { 
  CheckSquare, 
  Plus, 
  Sparkles, 
  Calendar, 
  Clock, 
  Trash2, 
  Filter, 
  Zap,
  CheckCircle2,
  AlertTriangle,
  Clock3,
  Brain,
  Layers
} from 'lucide-react';
import api from '../services/api';

export default function AssignmentsView({ assignments, summary = {}, onAddAssignment, onUpdateAssignment, onDeleteAssignment, onRefresh }) {
  const [showModal, setShowModal] = useState(false);
  const [statusFilter, setStatusFilter] = useState('All');
  const [isPrioritizing, setIsPrioritizing] = useState(false);
  const [advisorResult, setAdvisorResult] = useState(null);

  // Form State
  const [title, setTitle] = useState('');
  const [course, setCourse] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [priority, setPriority] = useState('High');
  const [estimatedHours, setEstimatedHours] = useState('3');
  const [category, setCategory] = useState('Assignment');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!title) return;
    onAddAssignment({ title, course: course || 'CS', dueDate, priority, estimatedHours, category });
    setTitle('');
    setCourse('');
    setDueDate('');
    setShowModal(false);
  };

  const handleRunAdvisor = async () => {
    setIsPrioritizing(true);
    setAdvisorResult(null);
    try {
      const res = await api.post('/assignments/advisor');
      if (res.data.success) {
        setAdvisorResult(res.data.data);
      }
    } catch (err) {
      console.error('Advisor error', err);
    } finally {
      setIsPrioritizing(false);
    }
  };

  const filteredAssignments = assignments.filter(a => {
    if (statusFilter === 'All') return true;
    if (statusFilter === 'Pending') return a.deadlineStatus !== 'COMPLETED' && a.status !== 'Completed';
    if (statusFilter === 'Completed') return a.deadlineStatus === 'COMPLETED' || a.status === 'Completed';
    if (statusFilter === 'Overdue') return a.deadlineStatus === 'OVERDUE';
    if (statusFilter === 'Due Soon') return a.deadlineStatus === 'DUE_SOON' || a.deadlineStatus === 'DUE_TODAY';
    return true;
  });

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

  const getDeadlineBadge = (daysRemaining, deadlineStatus) => {
    if (deadlineStatus === 'COMPLETED') return null;
    if (deadlineStatus === 'OVERDUE') {
      return <span style={{ color: '#f43f5e', fontWeight: 700, fontSize: '0.8rem' }}>⚠️ Overdue by {Math.abs(daysRemaining)} day(s)</span>;
    }
    if (deadlineStatus === 'DUE_TODAY') {
      return <span style={{ color: '#f59e0b', fontWeight: 700, fontSize: '0.8rem' }}>🚨 DUE TODAY</span>;
    }
    if (deadlineStatus === 'DUE_SOON') {
      return <span style={{ color: '#fbbf24', fontWeight: 600, fontSize: '0.8rem' }}>⏳ Due in {daysRemaining} day(s)</span>;
    }
    return <span style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>📅 Due in {daysRemaining} days</span>;
  };

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Header bar */}
      <div className="glass-card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <CheckSquare style={{ width: '22px', height: '22px', color: '#818cf8' }} /> Assignment & Deadline Intelligence
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '2px' }}>
            Deterministic deadline calculations, urgency classification, and AI-assisted prioritization.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button className="btn btn-secondary" onClick={handleRunAdvisor} disabled={isPrioritizing}>
            <Brain style={{ width: '16px', height: '16px', color: '#818cf8' }} /> {isPrioritizing ? 'Analyzing...' : 'AI Deadline Advisor'}
          </button>
          <button className="btn btn-primary" onClick={() => setShowModal(true)}>
            <Plus style={{ width: '16px', height: '16px' }} /> Add Assignment
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '14px' }}>
        <div className="glass-card" style={{ padding: '16px', textAlign: 'center' }}>
          <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: 600 }}>TOTAL</span>
          <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#e2e8f0', marginTop: '4px' }}>{summary.total || assignments.length}</div>
        </div>
        <div className="glass-card" style={{ padding: '16px', textAlign: 'center' }}>
          <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: 600 }}>PENDING</span>
          <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#818cf8', marginTop: '4px' }}>{summary.pending || 0}</div>
        </div>
        <div className="glass-card" style={{ padding: '16px', textAlign: 'center' }}>
          <span style={{ fontSize: '0.78rem', color: '#fca5a5', fontWeight: 600 }}>OVERDUE</span>
          <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#f43f5e', marginTop: '4px' }}>{summary.overdue || 0}</div>
        </div>
        <div className="glass-card" style={{ padding: '16px', textAlign: 'center' }}>
          <span style={{ fontSize: '0.78rem', color: '#fde047', fontWeight: 600 }}>DUE SOON</span>
          <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#f59e0b', marginTop: '4px' }}>{summary.dueSoon || 0}</div>
        </div>
        <div className="glass-card" style={{ padding: '16px', textAlign: 'center' }}>
          <span style={{ fontSize: '0.78rem', color: '#6ee7b7', fontWeight: 600 }}>COMPLETED</span>
          <div style={{ fontSize: '1.6rem', fontWeight: 800, color: '#10b981', marginTop: '4px' }}>{summary.completed || 0}</div>
        </div>
      </div>

      {/* AI Advisor Banner Result */}
      {advisorResult && (
        <div className="glass-card" style={{ padding: '20px', background: 'linear-gradient(135deg, rgba(30, 27, 75, 0.9), rgba(15, 23, 42, 0.95))', border: '1px solid rgba(129, 140, 248, 0.3)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#818cf8', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Brain style={{ width: '18px', height: '18px' }} /> AI Deadline Advisory Analysis
            </h3>
            {advisorResult.isFallback && <span className="badge badge-amber">Deterministic Advisory</span>}
          </div>
          <p style={{ fontSize: '0.9rem', color: '#e2e8f0', lineHeight: '1.5', whiteSpace: 'pre-line' }}>
            {advisorResult.recommendation}
          </p>
        </div>
      )}

      {/* Filter and Content List */}
      <div className="glass-card" style={{ padding: '24px' }}>
        
        {/* Filter controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '20px', flexWrap: 'wrap' }}>
          <Filter style={{ width: '16px', height: '16px', color: 'var(--text-muted)' }} />
          <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600 }}>Status Filter:</span>
          {['All', 'Pending', 'Completed', 'Overdue', 'Due Soon'].map(statusOpt => (
            <button
              key={statusOpt}
              className={`btn btn-sm ${statusFilter === statusOpt ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setStatusFilter(statusOpt)}
            >
              {statusOpt}
            </button>
          ))}
        </div>

        {/* List of assignments */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          {filteredAssignments.length === 0 ? (
            <div style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
              No assignments found matching this filter.
            </div>
          ) : (
            filteredAssignments.map(item => (
              <div 
                key={item.id}
                style={{
                  padding: '16px',
                  borderRadius: 'var(--radius-sm)',
                  background: item.status === 'Completed' || item.deadlineStatus === 'COMPLETED' ? 'rgba(255, 255, 255, 0.02)' : 'rgba(255, 255, 255, 0.04)',
                  border: item.urgency === 'CRITICAL' ? '1px solid rgba(244, 63, 94, 0.3)' : '1px solid var(--border-color)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  flexWrap: 'wrap',
                  gap: '12px',
                  opacity: item.status === 'Completed' || item.deadlineStatus === 'COMPLETED' ? 0.6 : 1
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                  <button 
                    onClick={() => onUpdateAssignment(item.id, { status: item.status === 'Completed' ? 'Pending' : 'Completed' })}
                    style={{ background: 'none', border: 'none', cursor: 'pointer', color: item.status === 'Completed' ? '#10b981' : 'var(--text-dim)' }}
                    title={item.status === 'Completed' ? 'Mark as Pending' : 'Mark as Completed'}
                  >
                    <CheckCircle2 style={{ width: '22px', height: '22px' }} />
                  </button>

                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span className="badge badge-cyan" style={{ fontSize: '0.75rem' }}>{item.course || item.subjectCode || 'CS'}</span>
                      <h4 style={{ fontSize: '0.98rem', fontWeight: 700, textDecoration: item.status === 'Completed' ? 'line-through' : 'none' }}>
                        {item.title}
                      </h4>
                    </div>

                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '6px', display: 'flex', gap: '14px', flexWrap: 'wrap', alignItems: 'center' }}>
                      {getDeadlineBadge(item.daysRemaining, item.deadlineStatus)}
                      <span>⏱️ ~{item.estimatedHours} hrs</span>
                      {item.description && <span style={{ color: 'var(--text-dim)' }}>"{item.description}"</span>}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  {getUrgencyBadge(item.urgency || item.priority)}

                  <button className="btn btn-secondary btn-sm" onClick={() => onDeleteAssignment(item.id)} style={{ color: '#fca5a5' }}>
                    <Trash2 style={{ width: '14px', height: '14px' }} />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

      </div>

      {/* Modal to Add Assignment */}
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
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, marginBottom: '16px' }}>Add New Assignment</h3>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Assignment Title</label>
                <input type="text" className="input-field" required placeholder="e.g. Algorithms Lab 3" value={title} onChange={(e) => setTitle(e.target.value)} />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Course Code / Name</label>
                  <input type="text" className="input-field" required placeholder="e.g. CS 201" value={course} onChange={(e) => setCourse(e.target.value)} />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Due Date</label>
                  <input type="date" className="input-field" required value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Priority Level</label>
                  <select className="input-field" value={priority} onChange={(e) => setPriority(e.target.value)}>
                    <option value="High">High</option>
                    <option value="Medium">Medium</option>
                    <option value="Low">Low</option>
                  </select>
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Est. Hours Needed</label>
                  <input type="number" className="input-field" value={estimatedHours} onChange={(e) => setEstimatedHours(e.target.value)} />
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '12px' }}>
                <button type="button" className="btn btn-secondary" style={{ width: '50%' }} onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" style={{ width: '50%' }}>
                  Save Assignment
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
