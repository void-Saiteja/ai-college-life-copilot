import React from 'react';
import { User, Mail, GraduationCap, ShieldCheck, Award } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function ProfilePage() {
  const { user } = useAuth();

  return (
    <div className="animate-fade-in" style={{ maxWidth: '600px', margin: '20px auto', width: '100%' }}>
      <div className="glass-card" style={{ padding: '32px' }}>
        
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div style={{
            width: '64px', height: '64px', borderRadius: '50%',
            background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            margin: '0 auto 12px', fontSize: '1.5rem', fontWeight: 800, color: '#fff'
          }}>
            {user?.name ? user.name[0] : 'A'}
          </div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800 }}>{user?.name || 'Alex Mercer'}</h2>
          <span className="badge badge-indigo" style={{ marginTop: '4px' }}>{user?.role || 'STUDENT'}</span>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
          <div style={{ padding: '12px 16px', borderRadius: '8px', background: 'rgba(255, 255, 255, 0.03)', border: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Mail style={{ width: '18px', height: '18px', color: '#818cf8' }} />
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Email Address</div>
              <div style={{ fontWeight: 600, fontSize: '0.92rem' }}>{user?.email || 'alex@student.edu'}</div>
            </div>
          </div>

          <div style={{ padding: '12px 16px', borderRadius: '8px', background: 'rgba(255, 255, 255, 0.03)', border: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <GraduationCap style={{ width: '18px', height: '18px', color: '#67e8f9' }} />
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Department & Academic Standing</div>
              <div style={{ fontWeight: 600, fontSize: '0.92rem' }}>{user?.department || 'Computer Science & Engineering'} • Semester {user?.semester || 4}</div>
            </div>
          </div>

          <div style={{ padding: '12px 16px', borderRadius: '8px', background: 'rgba(255, 255, 255, 0.03)', border: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Award style={{ width: '18px', height: '18px', color: '#6ee7b7' }} />
            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Cumulative GPA</div>
              <div style={{ fontWeight: 600, fontSize: '0.92rem', color: '#6ee7b7' }}>{user?.gpa || '3.85'} / 4.00 (First Class Distinction)</div>
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
