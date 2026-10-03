import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { 
  LayoutDashboard, 
  BrainCircuit, 
  Calendar, 
  Percent, 
  CheckSquare, 
  Award, 
  HelpCircle, 
  FileText, 
  Briefcase, 
  User, 
  ShieldCheck, 
  Sparkles,
  Bell,
  Clock,
  BookOpen,
  Wallet
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function Sidebar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();

  const studentNav = [
    { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { path: '/notifications', label: 'Notifications', icon: Bell },
    { path: '/schedule', label: 'Class Timetable', icon: Clock },
    { path: '/copilot', label: 'AI Copilot', icon: BrainCircuit, badge: 'AI' },
    { path: '/planner', label: 'Study Planner', icon: Calendar },
    { path: '/notes', label: 'Notes & AI Lab', icon: BookOpen, badge: 'AI' },
    { path: '/attendance', label: 'Attendance', icon: Percent },
    { path: '/assignments', label: 'Assignments', icon: CheckSquare },
    { path: '/exams', label: 'Exams', icon: Award },
    { path: '/quiz', label: 'AI Quiz', icon: HelpCircle },
    { path: '/budget', label: 'Student Budget', icon: Wallet },
    { path: '/documents', label: 'Documents & RAG', icon: FileText },
    { path: '/career', label: 'Career Assistant', icon: Briefcase },
    { path: '/profile', label: 'Profile', icon: User }
  ];

  const adminNav = [
    { path: '/admin', label: 'Admin Panel', icon: ShieldCheck, badge: 'ADMIN' },
    { path: '/documents', label: 'Document RAG Upload', icon: FileText },
    { path: '/dashboard', label: 'Student View', icon: LayoutDashboard }
  ];

  const items = user?.role === 'ADMIN' ? adminNav : studentNav;

  return (
    <aside className="glass-card" style={{ width: '250px', borderRadius: 'var(--radius-md)', padding: '16px', display: 'flex', flexDirection: 'column', gap: '6px', alignSelf: 'flex-start' }}>
      <div style={{ padding: '6px 10px', fontSize: '0.72rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
        Navigation
      </div>

      {items.map((item) => {
        const Icon = item.icon;
        const isActive = location.pathname === item.path;
        return (
          <button
            key={item.path}
            onClick={() => navigate(item.path)}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              width: '100%',
              padding: '10px 12px',
              borderRadius: 'var(--radius-sm)',
              border: 'none',
              background: isActive ? 'linear-gradient(90deg, rgba(99, 102, 241, 0.25), rgba(139, 92, 246, 0.15))' : 'transparent',
              color: isActive ? '#ffffff' : 'var(--text-muted)',
              cursor: 'pointer',
              fontWeight: isActive ? 700 : 500,
              fontSize: '0.9rem',
              transition: 'all 0.2s ease',
              borderLeft: isActive ? '3px solid #6366f1' : '3px solid transparent'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Icon style={{ width: '17px', height: '17px', color: isActive ? '#818cf8' : 'var(--text-dim)' }} />
              <span>{item.label}</span>
            </div>
            {item.badge && (
              <span className={`badge ${item.badge === 'ADMIN' ? 'badge-amber' : 'badge-indigo'}`} style={{ padding: '1px 5px', fontSize: '0.65rem' }}>
                {item.badge}
              </span>
            )}
          </button>
        );
      })}

      <div style={{ marginTop: 'auto', paddingTop: '16px' }}>
        <div className="glass-card" style={{ padding: '12px', background: 'rgba(99, 102, 241, 0.08)', borderColor: 'rgba(99, 102, 241, 0.2)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '4px' }}>
            <Sparkles style={{ width: '14px', height: '14px', color: '#a5b4fc' }} />
            <span style={{ fontSize: '0.8rem', fontWeight: 700, color: '#c7d2fe' }}>AI Copilot Ready</span>
          </div>
          <p style={{ fontSize: '0.74rem', color: 'var(--text-muted)', marginBottom: '8px' }}>
            Contextual LLM synchronized with records.
          </p>
          <button className="btn btn-primary btn-sm" style={{ width: '100%', fontSize: '0.78rem' }} onClick={() => navigate('/copilot')}>
            Open Chat
          </button>
        </div>
      </div>
    </aside>
  );
}
