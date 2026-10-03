import React from 'react';
import { 
  CheckSquare, 
  Clock, 
  BrainCircuit, 
  Wallet, 
  AlertTriangle, 
  TrendingUp, 
  Sparkles,
  ArrowRight,
  BookOpen,
  Calendar,
  Zap
} from 'lucide-react';

export default function DashboardView({ assignments, schedule, budget, notes, setActiveTab, onPrioritizeTasks }) {
  const pendingAssignments = assignments.filter(a => a.status !== 'Completed');
  const urgentAssignments = pendingAssignments.slice(0, 3);
  const totalHours = pendingAssignments.reduce((acc, c) => acc + Number(c.estimatedHours || 0), 0);

  let stressGaugeColor = '#10b981';
  let stressStatus = 'Optimal Focus';
  if (totalHours > 12) {
    stressGaugeColor = '#f43f5e';
    stressStatus = 'High Workload (Overload Alert)';
  } else if (totalHours > 6) {
    stressGaugeColor = '#f59e0b';
    stressStatus = 'Moderate Workload';
  }

  const spentPercent = budget.monthlyTarget ? Math.min(100, Math.round((budget.totalSpent / budget.monthlyTarget) * 100)) : 0;

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Welcome Banner */}
      <div className="glass-card" style={{
        padding: '24px 28px',
        background: 'linear-gradient(135deg, rgba(30, 27, 75, 0.8), rgba(15, 23, 42, 0.9))',
        border: '1px solid rgba(99, 102, 241, 0.25)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '16px'
      }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
            <Sparkles style={{ width: '20px', height: '20px', color: '#818cf8' }} />
            <h2 style={{ fontSize: '1.5rem', fontWeight: 800 }}>Welcome Back, Student!</h2>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.92rem' }}>
            Here is your daily academic snapshot. You have <strong style={{ color: '#a5b4fc' }}>{pendingAssignments.length} pending deadlines</strong> this week.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button className="btn btn-primary" onClick={() => setActiveTab('ai-lab')}>
            <BrainCircuit style={{ width: '18px', height: '18px' }} /> Launch AI Study Copilot
          </button>
          <button className="btn btn-secondary" onClick={() => setActiveTab('assignments')}>
            <Zap style={{ width: '18px', height: '18px', color: '#f59e0b' }} /> Eisenhower Prioritizer
          </button>
        </div>
      </div>

      {/* Metrics Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '16px' }}>
        
        {/* Metric 1 */}
        <div className="glass-card glass-card-interactive" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600 }}>Workload Stress Gauge</span>
            <div style={{ padding: '8px', borderRadius: '10px', background: 'rgba(99, 102, 241, 0.12)' }}>
              <TrendingUp style={{ width: '18px', height: '18px', color: '#818cf8' }} />
            </div>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: stressGaugeColor }}>
            {totalHours} hrs <span style={{ fontSize: '0.9rem', fontWeight: 500, color: 'var(--text-muted)' }}>needed</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '8px', fontSize: '0.8rem', color: stressGaugeColor }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: stressGaugeColor }} />
            {stressStatus}
          </div>
        </div>

        {/* Metric 2 */}
        <div className="glass-card glass-card-interactive" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600 }}>Urgent Assignments</span>
            <div style={{ padding: '8px', borderRadius: '10px', background: 'rgba(244, 63, 94, 0.12)' }}>
              <AlertTriangle style={{ width: '18px', height: '18px', color: '#f43f5e' }} />
            </div>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800 }}>
            {pendingAssignments.length} <span style={{ fontSize: '0.9rem', fontWeight: 500, color: 'var(--text-muted)' }}>tasks</span>
          </div>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '8px' }}>
            Next due: <strong style={{ color: '#fca5a5' }}>{urgentAssignments[0]?.dueDate || 'None'}</strong>
          </p>
        </div>

        {/* Metric 3 */}
        <div className="glass-card glass-card-interactive" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600 }}>Monthly Budget Spent</span>
            <div style={{ padding: '8px', borderRadius: '10px', background: 'rgba(6, 182, 212, 0.12)' }}>
              <Wallet style={{ width: '18px', height: '18px', color: '#06b6d4' }} />
            </div>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800 }}>
            ${budget.totalSpent?.toFixed(2) || '0.00'}{' '}
            <span style={{ fontSize: '0.9rem', fontWeight: 500, color: 'var(--text-muted)' }}>/ ${budget.monthlyTarget}</span>
          </div>
          <div style={{ width: '100%', height: '6px', background: 'rgba(255, 255, 255, 0.1)', borderRadius: '4px', marginTop: '10px', overflow: 'hidden' }}>
            <div style={{ width: `${spentPercent}%`, height: '100%', background: spentPercent > 85 ? '#f43f5e' : 'linear-gradient(90deg, #06b6d4, #10b981)', borderRadius: '4px' }} />
          </div>
        </div>

        {/* Metric 4 */}
        <div className="glass-card glass-card-interactive" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600 }}>Saved Study Notes</span>
            <div style={{ padding: '8px', borderRadius: '10px', background: 'rgba(139, 92, 246, 0.12)' }}>
              <BookOpen style={{ width: '18px', height: '18px', color: '#a78bfa' }} />
            </div>
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800 }}>
            {notes.length} <span style={{ fontSize: '0.9rem', fontWeight: 500, color: 'var(--text-muted)' }}>notes</span>
          </div>
          <p style={{ fontSize: '0.8rem', color: '#c7d2fe', marginTop: '8px' }}>
            Ready for AI Quiz & Flashcards
          </p>
        </div>

      </div>

      {/* Main Content Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '20px' }}>
        
        {/* Urgent Deadlines Widget */}
        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Clock style={{ width: '18px', height: '18px', color: '#818cf8' }} /> Priority Assignments
            </h3>
            <button className="btn btn-secondary btn-sm" onClick={() => setActiveTab('assignments')}>
              View All <ArrowRight style={{ width: '14px', height: '14px' }} />
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {pendingAssignments.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', padding: '20px 0', textAlign: 'center' }}>
                🎉 No pending assignments! You are all caught up.
              </p>
            ) : (
              urgentAssignments.map(item => (
                <div key={item.id} style={{
                  padding: '12px 14px',
                  borderRadius: 'var(--radius-sm)',
                  background: 'rgba(255, 255, 255, 0.03)',
                  border: '1px solid var(--border-color)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.92rem' }}>{item.title}</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                      {item.course} • Due: {item.dueDate}
                    </div>
                  </div>
                  <span className={`badge ${item.priority === 'High' ? 'badge-rose' : item.priority === 'Medium' ? 'badge-amber' : 'badge-indigo'}`}>
                    {item.priority}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Today's Course Schedule Widget */}
        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Calendar style={{ width: '18px', height: '18px', color: '#06b6d4' }} /> Weekly Class Schedule
            </h3>
            <span className="badge badge-cyan">3 Classes</span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {schedule.map(item => (
              <div key={item.id} style={{
                padding: '12px 14px',
                borderRadius: 'var(--radius-sm)',
                background: 'rgba(6, 182, 212, 0.05)',
                border: '1px solid rgba(6, 182, 212, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}>
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.92rem', color: '#67e8f9' }}>{item.course}</div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                    📍 {item.room} • 👤 {item.instructor}
                  </div>
                </div>
                <div style={{ textAlign: 'right', fontSize: '0.8rem', color: '#e0f2fe', fontWeight: 600 }}>
                  <div>{item.time}</div>
                  <div style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>{item.days?.join(', ')}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

      </div>

    </div>
  );
}
