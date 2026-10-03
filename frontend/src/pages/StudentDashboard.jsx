import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Sparkles, 
  BrainCircuit, 
  BookOpen, 
  Clock, 
  Calendar, 
  Award, 
  Bell, 
  TrendingUp, 
  CheckCircle2, 
  AlertCircle,
  ArrowRight,
  Wallet
} from 'lucide-react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function StudentDashboard() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const [dashboardData, setDashboardData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    async function loadStats() {
      try {
        const res = await api.get('/student/dashboard');
        if (res.data.success) {
          setDashboardData(res.data.data);
        }
      } catch (err) {
        console.warn('Backend load stats, using local context state fallback.');
        setDashboardData({
          student: { name: user?.name || 'Alex Mercer', semester: 4, department: 'Computer Science' },
          subjects: [
            { code: 'CS 201', name: 'Data Structures & Algorithms', instructor: 'Dr. Alan Turing' },
            { code: 'CS 340', name: 'Database Management Systems', instructor: 'Dr. Grace Hopper' },
            { code: 'MATH 220', name: 'Calculus III', instructor: 'Prof. Katherine Johnson' }
          ],
          attendance: [
            { subject: 'Data Structures', percentage: 89, status: 'Good Standing' },
            { subject: 'Database Systems', percentage: 90, status: 'Good Standing' }
          ],
          upcomingAssignments: [
            { id: '1', title: 'B-Trees & Red-Black Tree Implementation', subject: 'Data Structures', dueDate: new Date(Date.now() + 86400000 * 2).toISOString().split('T')[0], priority: 'High' }
          ],
          upcomingExams: [
            { id: '1', title: 'DSA Midterm Examination', subject: 'Data Structures', examDate: new Date(Date.now() + 86400000 * 7).toISOString().split('T')[0], location: 'Hall A2' }
          ],
          studyPlan: {
            plan: { title: 'Midterm Sprint Plan 2026', status: 'Active' },
            sessions: [
              { topic: 'Binary Search Trees & AVL Rotations', session_date: new Date().toISOString().split('T')[0], status: 'Pending' }
            ]
          },
          recentQuizScore: '80% in Data Structures',
          notifications: [
            { id: '1', title: 'Assignment Due Soon', message: 'B-Trees implementation is due in 2 days.', type: 'assignment' }
          ]
        });
      } finally {
        setLoading(false);
      }
    }

    loadStats();
  }, [user]);

  if (loading) {
    return (
      <div className="glass-card" style={{ padding: '60px', textAlign: 'center', color: 'var(--text-muted)' }}>
        ⏳ Loading your personalized student dashboard...
      </div>
    );
  }

  const { student, subjects, attendance, upcomingAssignments, upcomingExams, studyPlan, recentQuizScore, notifications } = dashboardData || {};

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Welcome Banner Card */}
      <div className="glass-card" style={{
        padding: '24px 28px',
        background: 'linear-gradient(135deg, rgba(30, 27, 75, 0.9), rgba(15, 23, 42, 0.95))',
        border: '1px solid rgba(99, 102, 241, 0.3)',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        flexWrap: 'wrap',
        gap: '16px'
      }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
            <Sparkles style={{ width: '20px', height: '20px', color: '#818cf8' }} />
            <h2 style={{ fontSize: '1.4rem', fontWeight: 800 }}>Welcome Back, {student?.name}!</h2>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem' }}>
            Semester {student?.semester} • {student?.department} • GPA: <strong style={{ color: '#6ee7b7' }}>{student?.gpa || '3.85'}</strong>
          </p>
        </div>

        {/* AI Copilot Shortcut */}
        <button className="btn btn-primary btn-lg" onClick={() => navigate('/copilot')}>
          <BrainCircuit style={{ width: '20px', height: '20px' }} /> Ask AI Copilot
        </button>
      </div>

      {/* Primary KPI Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px' }}>
        
        {/* KPI 1: Enrolled Subjects */}
        <div className="glass-card glass-card-interactive" style={{ padding: '20px' }} onClick={() => navigate('/attendance')}>
          <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', fontWeight: 600 }}>Enrolled Subjects</div>
          <div style={{ fontSize: '1.7rem', fontWeight: 800, marginTop: '4px' }}>{subjects?.length || 0} Courses</div>
          <div style={{ fontSize: '0.78rem', color: '#818cf8', marginTop: '6px' }}>View Course Breakdown →</div>
        </div>

        {/* KPI 2: Overall Attendance */}
        <div className="glass-card glass-card-interactive" style={{ padding: '20px' }} onClick={() => navigate('/attendance')}>
          <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', fontWeight: 600 }}>Avg. Attendance</div>
          <div style={{ fontSize: '1.7rem', fontWeight: 800, color: '#6ee7b7', marginTop: '4px' }}>
            {attendance?.[0]?.percentage || 89}%
          </div>
          <div style={{ fontSize: '0.78rem', color: '#6ee7b7', marginTop: '6px' }}>Good Standing (&gt;75%)</div>
        </div>

        {/* KPI 3: Recent Quiz Score */}
        <div className="glass-card glass-card-interactive" style={{ padding: '20px' }} onClick={() => navigate('/quiz')}>
          <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', fontWeight: 600 }}>Recent AI Quiz</div>
          <div style={{ fontSize: '1.4rem', fontWeight: 800, color: '#fde047', marginTop: '4px' }}>
            {recentQuizScore || '80% Score'}
          </div>
          <div style={{ fontSize: '0.78rem', color: '#fde047', marginTop: '6px' }}>Practice AI Quiz →</div>
        </div>

        {/* KPI 4: Pending Assignments */}
        <div className="glass-card glass-card-interactive" style={{ padding: '20px' }} onClick={() => navigate('/assignments')}>
          <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', fontWeight: 600 }}>Due Deadlines</div>
          <div style={{ fontSize: '1.7rem', fontWeight: 800, color: '#fca5a5', marginTop: '4px' }}>
            {upcomingAssignments?.length || 0} Pending
          </div>
          <div style={{ fontSize: '0.78rem', color: '#fca5a5', marginTop: '6px' }}>Next: {upcomingAssignments?.[0]?.dueDate || '2 Days'}</div>
        </div>

      </div>

      {/* Quick Launch Suite */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px' }}>
        <div 
          className="glass-card glass-card-interactive" 
          style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer', background: 'linear-gradient(135deg, rgba(99, 102, 241, 0.1), rgba(15, 23, 42, 0.6))' }}
          onClick={() => navigate('/schedule')}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div style={{ width: '40px', height: '40px', borderRadius: '10px', background: 'rgba(99, 102, 241, 0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Clock style={{ width: '20px', height: '20px', color: '#818cf8' }} />
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>Class Timetable</div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Daily lectures & rooms</div>
            </div>
          </div>
          <ArrowRight style={{ width: '16px', height: '16px', color: '#818cf8' }} />
        </div>

        <div 
          className="glass-card glass-card-interactive" 
          style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer', background: 'linear-gradient(135deg, rgba(168, 85, 247, 0.1), rgba(15, 23, 42, 0.6))' }}
          onClick={() => navigate('/notes')}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div style={{ width: '40px', height: '40px', borderRadius: '10px', background: 'rgba(168, 85, 247, 0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <BookOpen style={{ width: '20px', height: '20px', color: '#c084fc' }} />
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>Notes & AI Lab</div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Summarizer & flashcards</div>
            </div>
          </div>
          <ArrowRight style={{ width: '16px', height: '16px', color: '#c084fc' }} />
        </div>

        <div 
          className="glass-card glass-card-interactive" 
          style={{ padding: '16px 20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', cursor: 'pointer', background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(15, 23, 42, 0.6))' }}
          onClick={() => navigate('/budget')}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div style={{ width: '40px', height: '40px', borderRadius: '10px', background: 'rgba(16, 185, 129, 0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <Wallet style={{ width: '20px', height: '20px', color: '#34d399' }} />
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>Student Budget</div>
              <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Expenses & meal planner</div>
            </div>
          </div>
          <ArrowRight style={{ width: '16px', height: '16px', color: '#34d399' }} />
        </div>
      </div>

      {/* Main Grid: Assignments, Exams & Notifications */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))', gap: '20px' }}>
        
        {/* Upcoming Assignments Card */}
        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
            <h3 style={{ fontSize: '1.05rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Clock style={{ width: '18px', height: '18px', color: '#818cf8' }} /> Upcoming Assignments
            </h3>
            <button className="btn btn-secondary btn-sm" onClick={() => navigate('/assignments')}>
              View All
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {upcomingAssignments?.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem' }}>No pending assignments!</p>
            ) : (
              upcomingAssignments?.slice(0, 3).map(a => (
                <div key={a.id} style={{ padding: '12px', borderRadius: '8px', background: 'rgba(255, 255, 255, 0.03)', border: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <div style={{ fontWeight: 600, fontSize: '0.9rem' }}>{a.title}</div>
                    <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '2px' }}>{a.subject} • Due: {a.dueDate}</div>
                  </div>
                  <span className={`badge ${a.priority === 'High' ? 'badge-rose' : 'badge-amber'}`}>{a.priority}</span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Upcoming Exams Card */}
        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
            <h3 style={{ fontSize: '1.05rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Calendar style={{ width: '18px', height: '18px', color: '#06b6d4' }} /> Upcoming Examinations
            </h3>
            <button className="btn btn-secondary btn-sm" onClick={() => navigate('/exams')}>
              Schedule
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {upcomingExams?.slice(0, 2).map(e => (
              <div key={e.id} style={{ padding: '12px', borderRadius: '8px', background: 'rgba(6, 182, 212, 0.05)', border: '1px solid rgba(6, 182, 212, 0.15)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.9rem', color: '#67e8f9' }}>{e.title}</div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '2px' }}>📍 {e.location || 'Hall A2'}</div>
                </div>
                <div style={{ textAlign: 'right', fontSize: '0.8rem', fontWeight: 600, color: '#e0f2fe' }}>
                  <div>{e.examDate}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Study Plan & Notifications Card */}
        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '14px' }}>
            <h3 style={{ fontSize: '1.05rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Bell style={{ width: '18px', height: '18px', color: '#f59e0b' }} /> Academic Notifications
            </h3>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {notifications?.map((n, idx) => (
              <div key={idx} style={{ padding: '10px 12px', borderRadius: '6px', background: 'rgba(255, 255, 255, 0.03)', fontSize: '0.85rem' }}>
                <div style={{ fontWeight: 700, color: '#fde047' }}>{n.title}</div>
                <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '2px' }}>{n.message}</div>
              </div>
            ))}
          </div>
        </div>

      </div>

    </div>
  );
}
