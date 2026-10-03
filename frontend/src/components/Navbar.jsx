import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { GraduationCap, Sparkles, Bell, LogOut, User as UserIcon } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

export default function Navbar() {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [showNotifMenu, setShowNotifMenu] = useState(false);

  useEffect(() => {
    async function loadNotifications() {
      try {
        const res = await api.get('/notifications');
        if (res.data.success) {
          setNotifications(res.data.data.notifications || []);
          setUnreadCount(res.data.data.unreadCount || 0);
        }
      } catch (err) {
        // Silent fallback
      }
    }
    loadNotifications();
  }, []);

  const handleMarkAllRead = async () => {
    try {
      await api.put('/notifications/read-all');
      setUnreadCount(0);
      setNotifications(prev => prev.map(n => ({ ...n, is_read: true })));
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <header className="glass-card" style={{ borderRadius: 0, borderTop: 'none', borderLeft: 'none', borderRight: 'none', padding: '14px 24px', position: 'sticky', top: 0, zIndex: 50 }}>
      <div style={{ maxWidth: '1400px', margin: '0 auto', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        
        {/* Brand */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', cursor: 'pointer' }} onClick={() => navigate('/')}>
          <div style={{
            width: '42px', height: '42px', borderRadius: '12px',
            background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 4px 20px rgba(99, 102, 241, 0.4)'
          }}>
            <GraduationCap style={{ width: '24px', height: '24px', color: '#fff' }} />
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <h1 style={{ fontSize: '1.25rem', fontWeight: 800, background: 'linear-gradient(90deg, #ffffff, #c7d2fe)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
                AI College Copilot
              </h1>
              <span className="badge badge-indigo">
                <Sparkles style={{ width: '12px', height: '12px' }} /> v2.0
              </span>
            </div>
            <p style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>Smart Student Life & Academic Intelligence</p>
          </div>
        </div>

        {/* User controls */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', position: 'relative' }}>
          
          {/* Notification Center Bell */}
          <button 
            className="btn btn-secondary btn-sm" 
            style={{ padding: '8px 10px', borderRadius: '50%', position: 'relative' }}
            onClick={() => setShowNotifMenu(!showNotifMenu)}
            title="Notifications"
          >
            <Bell style={{ width: '18px', height: '18px' }} />
            {unreadCount > 0 && (
              <span style={{ 
                position: 'absolute', 
                top: '-5px', 
                right: '-5px', 
                backgroundColor: '#f43f5e', 
                color: '#fff',
                fontSize: '0.65rem',
                fontWeight: 800,
                borderRadius: '10px',
                padding: '1px 5px',
                boxShadow: '0 0 8px rgba(244, 63, 94, 0.6)' 
              }}>
                {unreadCount}
              </span>
            )}
          </button>

          {/* Notification Menu Dropdown */}
          {showNotifMenu && (
            <div className="glass-card" style={{ position: 'absolute', top: '48px', right: '120px', width: '340px', padding: '16px', zIndex: 100, border: '1px solid #6366f1' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                <span style={{ fontWeight: 700, fontSize: '0.9rem', color: '#f8fafc' }}>
                  Notifications ({unreadCount} unread)
                </span>
                {unreadCount > 0 && (
                  <button className="btn btn-secondary btn-sm" style={{ fontSize: '0.72rem', padding: '2px 8px' }} onClick={handleMarkAllRead}>
                    Mark All Read
                  </button>
                )}
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '260px', overflowY: 'auto' }}>
                {notifications.length === 0 ? (
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', textAlign: 'center', padding: '16px 0' }}>
                    No unread notifications
                  </div>
                ) : (
                  notifications.slice(0, 5).map((n, idx) => (
                    <div 
                      key={idx} 
                      onClick={() => {
                        navigate('/notifications');
                        setShowNotifMenu(false);
                      }}
                      style={{ 
                        padding: '10px 12px', 
                        borderRadius: '6px', 
                        background: n.is_read ? 'transparent' : 'rgba(99, 102, 241, 0.1)', 
                        border: '1px solid var(--border-color)', 
                        fontSize: '0.8rem',
                        cursor: 'pointer'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2px' }}>
                        <span style={{ fontWeight: 700, color: '#a5b4fc' }}>{n.title}</span>
                        <span className={`badge ${n.priority === 'CRITICAL' ? 'badge-rose' : n.priority === 'HIGH' ? 'badge-amber' : 'badge-indigo'}`} style={{ fontSize: '0.62rem', padding: '1px 4px' }}>
                          {n.priority}
                        </span>
                      </div>
                      <div style={{ fontSize: '0.76rem', color: 'var(--text-muted)', lineHeight: '1.4' }}>{n.message}</div>
                    </div>
                  ))
                )}
              </div>

              <div style={{ marginTop: '12px', paddingTop: '10px', borderTop: '1px solid var(--border-color)', textAlign: 'center' }}>
                <button 
                  className="btn btn-primary btn-sm" 
                  style={{ width: '100%', fontSize: '0.8rem' }}
                  onClick={() => {
                    navigate('/notifications');
                    setShowNotifMenu(false);
                  }}
                >
                  View All Notifications
                </button>
              </div>
            </div>
          )}

          {user ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{ textAlign: 'right', cursor: 'pointer' }} onClick={() => navigate('/profile')}>
                <div style={{ fontSize: '0.88rem', fontWeight: 700 }}>{user.name}</div>
                <span className="badge badge-indigo" style={{ padding: '1px 6px', fontSize: '0.68rem' }}>
                  {user.role}
                </span>
              </div>
              <button className="btn btn-secondary btn-sm" onClick={logout} title="Sign Out">
                <LogOut style={{ width: '16px', height: '16px', color: '#fca5a5' }} />
              </button>
            </div>
          ) : (
            <button className="btn btn-primary btn-sm" onClick={() => navigate('/login')}>
              Sign In
            </button>
          )}

        </div>

      </div>
    </header>
  );
}
