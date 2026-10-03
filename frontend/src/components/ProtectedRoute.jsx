import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children, requiredRole }) {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="glass-card" style={{ margin: '40px auto', padding: '40px', maxWidth: '400px', textAlign: 'center', color: 'var(--text-muted)' }}>
        ⏳ Loading Security Context...
      </div>
    );
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRole && user.role !== requiredRole) {
    return (
      <div className="glass-card" style={{ margin: '60px auto', padding: '40px', maxWidth: '500px', textAlign: 'center' }}>
        <h2 style={{ color: '#f43f5e', fontSize: '1.4rem', fontWeight: 800, marginBottom: '8px' }}>🚫 403 Forbidden Access</h2>
        <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginBottom: '16px' }}>
          This page requires the <strong>{requiredRole}</strong> role. Your current role is <strong>{user.role}</strong>.
        </p>
        <button className="btn btn-primary" onClick={() => window.history.back()}>
          Go Back
        </button>
      </div>
    );
  }

  return children;
}
