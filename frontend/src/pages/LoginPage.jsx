import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { GraduationCap, Lock, Mail, ShieldAlert, Sparkles } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const navigate = useNavigate();
  const { login } = useAuth();
  
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSubmitting(true);

    const res = await login(email, password);
    setSubmitting(false);

    if (res.success) {
      navigate(res.user.role === 'ADMIN' ? '/admin' : '/dashboard');
    } else {
      setError(res.error);
    }
  };

  const handleDemoStudent = async () => {
    setError('');
    setEmail('alex@student.edu');
    setPassword('Student@123');
    setSubmitting(true);
    const res = await login('alex@student.edu', 'Student@123');
    setSubmitting(false);
    if (res.success) {
      navigate('/dashboard');
    } else {
      setError(res.error);
    }
  };

  const handleDemoAdmin = async () => {
    setError('');
    setEmail('admin@college.edu');
    setPassword('Admin@123');
    setSubmitting(true);
    const res = await login('admin@college.edu', 'Admin@123');
    setSubmitting(false);
    if (res.success) {
      navigate('/admin');
    } else {
      setError(res.error);
    }
  };

  return (
    <div className="animate-fade-in" style={{ maxWidth: '440px', margin: '40px auto', width: '100%' }}>
      <div className="glass-card" style={{ padding: '32px' }}>
        
        <div style={{ textAlign: 'center', marginBottom: '24px' }}>
          <div style={{
            width: '48px', height: '48px', borderRadius: '14px',
            background: 'linear-gradient(135deg, #6366f1, #8b5cf6)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            margin: '0 auto 12px', boxShadow: '0 4px 20px rgba(99, 102, 241, 0.4)'
          }}>
            <GraduationCap style={{ width: '28px', height: '28px', color: '#fff' }} />
          </div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800 }}>Welcome Back</h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '4px' }}>Sign in to your AI College Life Copilot account</p>
        </div>

        {error && (
          <div style={{ padding: '10px 14px', borderRadius: '8px', background: 'rgba(244, 63, 94, 0.15)', border: '1px solid rgba(244, 63, 94, 0.3)', color: '#fca5a5', fontSize: '0.85rem', marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <ShieldAlert style={{ width: '16px', height: '16px' }} /> {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Email Address</label>
            <div style={{ position: 'relative' }}>
              <Mail style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', width: '16px', height: '16px', color: 'var(--text-dim)' }} />
              <input 
                type="email" 
                className="input-field" 
                style={{ paddingLeft: '38px' }}
                required 
                placeholder="student@college.edu" 
                value={email} 
                onChange={(e) => setEmail(e.target.value)} 
              />
            </div>
          </div>

          <div>
            <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Password</label>
            <div style={{ position: 'relative' }}>
              <Lock style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', width: '16px', height: '16px', color: 'var(--text-dim)' }} />
              <input 
                type="password" 
                className="input-field" 
                style={{ paddingLeft: '38px' }}
                required 
                placeholder="••••••••" 
                value={password} 
                onChange={(e) => setPassword(e.target.value)} 
              />
            </div>
          </div>

          <button type="submit" className="btn btn-primary btn-lg" disabled={submitting} style={{ width: '100%', marginTop: '8px' }}>
            {submitting ? 'Authenticating...' : 'Sign In'}
          </button>
        </form>

        {/* Quick One-Click Demo Logins */}
        <div style={{ marginTop: '24px', paddingTop: '18px', borderTop: '1px solid var(--border-color)' }}>
          <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', textAlign: 'center', marginBottom: '10px', fontWeight: 600 }}>
            ⚡ Instant One-Click Demo Sign In
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            <button className="btn btn-secondary btn-sm" onClick={handleDemoStudent}>
              Alex Mercer (Student)
            </button>
            <button className="btn btn-secondary btn-sm" onClick={handleDemoAdmin}>
              Dr. Sarah (Admin)
            </button>
          </div>
        </div>

        <p style={{ textAlign: 'center', fontSize: '0.85rem', color: 'var(--text-muted)', marginTop: '20px' }}>
          Don't have an account? <Link to="/register" style={{ color: '#818cf8', fontWeight: 600 }}>Register as Student</Link>
        </p>

      </div>
    </div>
  );
}
