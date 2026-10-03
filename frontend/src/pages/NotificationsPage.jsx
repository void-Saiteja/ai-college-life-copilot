import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Bell, 
  CheckCircle2, 
  Trash2, 
  AlertTriangle, 
  Clock, 
  Calendar, 
  Award, 
  HelpCircle, 
  Briefcase, 
  Megaphone, 
  Sparkles,
  ExternalLink,
  Filter,
  Check,
  RefreshCw
} from 'lucide-react';
import api from '../services/api';

const TYPE_ICONS = {
  ATTENDANCE: AlertTriangle,
  ASSIGNMENT: Clock,
  EXAM: Calendar,
  QUIZ: HelpCircle,
  STUDY_PLAN: Award,
  CAREER: Briefcase,
  ADMIN_ANNOUNCEMENT: Megaphone,
  SYSTEM: Sparkles
};

const TYPE_ROUTES = {
  ATTENDANCE: '/attendance',
  ASSIGNMENT: '/assignments',
  EXAM: '/exams',
  QUIZ: '/quiz',
  STUDY_PLAN: '/planner',
  CAREER: '/career',
  ADMIN_ANNOUNCEMENT: '/dashboard',
  SYSTEM: '/dashboard'
};

export default function NotificationsPage() {
  const navigate = useNavigate();
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('all'); // 'all' | 'unread' | 'read'
  const [typeFilter, setTypeFilter] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);

  const fetchNotifications = async () => {
    try {
      setLoading(true);
      const params = {};
      if (statusFilter !== 'all') params.status = statusFilter;
      if (typeFilter) params.type = typeFilter;

      const res = await api.get('/notifications', { params });
      if (res.data.success) {
        setNotifications(res.data.data.notifications || []);
        setUnreadCount(res.data.data.unreadCount || 0);
      }
    } catch (err) {
      console.error('Failed to load notifications:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, [statusFilter, typeFilter]);

  const handleMarkAsRead = async (id, e) => {
    if (e) e.stopPropagation();
    try {
      const res = await api.put(`/notifications/${id}/read`);
      if (res.data.success) {
        setNotifications(prev => prev.map(n => n.id === id ? { ...n, is_read: true } : n));
        setUnreadCount(prev => Math.max(0, prev - 1));
      }
    } catch (err) {
      console.error('Failed to mark read:', err);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      const res = await api.put('/notifications/read-all');
      if (res.data.success) {
        setNotifications(prev => prev.map(n => ({ ...n, is_read: true })));
        setUnreadCount(0);
      }
    } catch (err) {
      console.error('Failed to mark all read:', err);
    }
  };

  const handleDelete = async (id, e) => {
    if (e) e.stopPropagation();
    try {
      const res = await api.delete(`/notifications/${id}`);
      if (res.data.success) {
        const deleted = notifications.find(n => n.id === id);
        setNotifications(prev => prev.filter(n => n.id !== id));
        if (deleted && !deleted.is_read) {
          setUnreadCount(prev => Math.max(0, prev - 1));
        }
      }
    } catch (err) {
      console.error('Failed to delete notification:', err);
    }
  };

  const handleGenerateNotifications = async () => {
    try {
      setIsGenerating(true);
      const res = await api.post('/notifications/generate');
      if (res.data.success) {
        fetchNotifications();
      }
    } catch (err) {
      console.error('Failed to generate notifications:', err);
    } finally {
      setIsGenerating(false);
    }
  };

  const handleNavigate = (type) => {
    const route = TYPE_ROUTES[type] || '/dashboard';
    navigate(route);
  };

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Header bar */}
      <div className="glass-card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Bell style={{ width: '24px', height: '24px', color: '#f59e0b' }} /> Student Notification Intelligence
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '2px' }}>
            Real-time proactive academic alerts for attendance thresholds, assignment deadlines, and exam schedules.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
          <button 
            className="btn btn-secondary"
            disabled={isGenerating}
            onClick={handleGenerateNotifications}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <RefreshCw style={{ width: '15px', height: '15px' }} className={isGenerating ? 'animate-spin' : ''} />
            {isGenerating ? 'Scanning...' : 'Sync Alerts'}
          </button>
          {unreadCount > 0 && (
            <button className="btn btn-primary" onClick={handleMarkAllRead}>
              <CheckCircle2 style={{ width: '15px', height: '15px' }} /> Mark All Read ({unreadCount})
            </button>
          )}
        </div>
      </div>

      {/* Filter and Control Bar */}
      <div className="glass-card" style={{ padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '14px' }}>
        {/* Status filters */}
        <div style={{ display: 'flex', background: 'rgba(0,0,0,0.25)', padding: '4px', borderRadius: '10px', gap: '4px' }}>
          <button 
            className={`btn ${statusFilter === 'all' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => setStatusFilter('all')}
          >
            All ({notifications.length})
          </button>
          <button 
            className={`btn ${statusFilter === 'unread' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => setStatusFilter('unread')}
          >
            Unread ({unreadCount})
          </button>
          <button 
            className={`btn ${statusFilter === 'read' ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            onClick={() => setStatusFilter('read')}
          >
            Read
          </button>
        </div>

        {/* Type Filter Dropdown */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Filter style={{ width: '15px', height: '15px', color: 'var(--text-muted)' }} />
          <select 
            className="input-field" 
            style={{ width: '200px', height: '36px', fontSize: '0.85rem' }}
            value={typeFilter}
            onChange={e => setTypeFilter(e.target.value)}
          >
            <option value="">All Alert Categories</option>
            <option value="ATTENDANCE">Attendance Alerts</option>
            <option value="ASSIGNMENT">Assignment Deadlines</option>
            <option value="EXAM">Upcoming Exams</option>
            <option value="QUIZ">Quiz Weak Topics</option>
            <option value="STUDY_PLAN">Study Planner Sessions</option>
            <option value="CAREER">Career Skill Gaps</option>
            <option value="ADMIN_ANNOUNCEMENT">Campus Announcements</option>
          </select>
        </div>
      </div>

      {/* Notifications List */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
        {loading ? (
          <div className="glass-card" style={{ padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
            ⏳ Loading notifications...
          </div>
        ) : notifications.length === 0 ? (
          <div className="glass-card" style={{ padding: '60px', textAlign: 'center' }}>
            <Bell style={{ width: '40px', height: '40px', color: 'var(--text-muted)', margin: '0 auto 12px', opacity: 0.5 }} />
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#f8fafc', marginBottom: '6px' }}>No Notifications Found</h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '16px' }}>
              You're all caught up! Click below to sync with your current academic schedule.
            </p>
            <button className="btn btn-secondary btn-sm" onClick={handleGenerateNotifications}>
              <RefreshCw style={{ width: '14px', height: '14px' }} /> Sync With Academic Data
            </button>
          </div>
        ) : (
          notifications.map(n => {
            const Icon = TYPE_ICONS[n.type] || Bell;
            const priorityClass = n.priority === 'CRITICAL' ? 'badge-rose' : n.priority === 'HIGH' ? 'badge-amber' : n.priority === 'MEDIUM' ? 'badge-indigo' : 'badge-cyan';

            return (
              <div 
                key={n.id}
                className="glass-card animate-fade-in"
                style={{
                  padding: '16px 20px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: '16px',
                  borderLeft: n.priority === 'CRITICAL' ? '4px solid #f43f5e' : n.priority === 'HIGH' ? '4px solid #f59e0b' : '4px solid #6366f1',
                  background: n.is_read ? 'rgba(255, 255, 255, 0.02)' : 'rgba(99, 102, 241, 0.08)'
                }}
              >
                <div style={{ display: 'flex', gap: '14px', alignItems: 'flex-start', flex: 1 }}>
                  <div style={{
                    width: '38px', height: '38px', borderRadius: '10px',
                    background: n.priority === 'CRITICAL' ? 'rgba(244, 63, 94, 0.15)' : 'rgba(99, 102, 241, 0.15)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    flexShrink: 0
                  }}>
                    <Icon style={{ width: '20px', height: '20px', color: n.priority === 'CRITICAL' ? '#f43f5e' : '#818cf8' }} />
                  </div>

                  <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                      <span className={`badge ${priorityClass}`} style={{ fontSize: '0.65rem' }}>{n.priority}</span>
                      <span className="badge badge-indigo" style={{ fontSize: '0.65rem' }}>{n.type.replace('_', ' ')}</span>
                      {!n.is_read && (
                        <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#38bdf8' }} title="Unread" />
                      )}
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        {new Date(n.created_at).toLocaleDateString()} at {new Date(n.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>

                    <h4 style={{ fontSize: '0.95rem', fontWeight: 700, color: n.is_read ? '#cbd5e1' : '#f8fafc', marginTop: '2px' }}>
                      {n.title}
                    </h4>
                    <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', lineHeight: '1.5' }}>
                      {n.message}
                    </p>
                  </div>
                </div>

                {/* Actions */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <button 
                    className="btn btn-secondary btn-sm"
                    onClick={() => handleNavigate(n.type)}
                    title="Navigate to module"
                    style={{ display: 'flex', alignItems: 'center', gap: '4px' }}
                  >
                    <ExternalLink style={{ width: '13px', height: '13px' }} /> View
                  </button>

                  {!n.is_read && (
                    <button 
                      className="btn btn-secondary btn-sm"
                      onClick={(e) => handleMarkAsRead(n.id, e)}
                      title="Mark as read"
                    >
                      <Check style={{ width: '13px', height: '13px' }} />
                    </button>
                  )}

                  <button 
                    className="btn btn-secondary btn-sm"
                    onClick={(e) => handleDelete(n.id, e)}
                    title="Delete notification"
                    style={{ color: '#f87171' }}
                  >
                    <Trash2 style={{ width: '13px', height: '13px' }} />
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>

    </div>
  );
}
