import React from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  GraduationCap, 
  Sparkles, 
  BrainCircuit, 
  Calendar, 
  CheckSquare, 
  FileText, 
  Briefcase, 
  ShieldCheck,
  ArrowRight
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

export default function HomePage() {
  const navigate = useNavigate();
  const { user } = useAuth();

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '40px', paddingBottom: '40px' }}>
      
      {/* Hero Section */}
      <div className="glass-card" style={{
        padding: '50px 36px',
        textAlign: 'center',
        background: 'linear-gradient(135deg, rgba(30, 27, 75, 0.95), rgba(15, 23, 42, 0.98))',
        border: '1px solid rgba(99, 102, 241, 0.3)',
        borderRadius: 'var(--radius-lg)'
      }}>
        <span className="badge badge-indigo" style={{ padding: '6px 14px', fontSize: '0.85rem', marginBottom: '16px' }}>
          <Sparkles style={{ width: '14px', height: '14px' }} /> Ultimate Academic Operating System v2.0
        </span>
        <h1 style={{ fontSize: '2.8rem', fontWeight: 800, margin: '16px 0', lineHeight: '1.2', background: 'linear-gradient(90deg, #ffffff, #c7d2fe, #818cf8)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
          AI College Life Copilot
        </h1>
        <p style={{ maxWidth: '720px', margin: '0 auto 28px', color: 'var(--text-muted)', fontSize: '1.08rem', lineHeight: '1.6' }}>
          An all-in-one AI assistant, smart study planner, RAG document search engine, attendance calculator, and career resume analyzer designed for modern college students.
        </p>

        <div style={{ display: 'flex', justifyContent: 'center', gap: '16px', flexWrap: 'wrap' }}>
          {user ? (
            <button className="btn btn-primary btn-lg" onClick={() => navigate(user.role === 'ADMIN' ? '/admin' : '/dashboard')}>
              Go to {user.role === 'ADMIN' ? 'Admin Dashboard' : 'Student Dashboard'} <ArrowRight style={{ width: '18px', height: '18px' }} />
            </button>
          ) : (
            <>
              <button className="btn btn-primary btn-lg" onClick={() => navigate('/login')}>
                Get Started / Sign In
              </button>
              <button className="btn btn-secondary btn-lg" onClick={() => navigate('/register')}>
                Create Student Account
              </button>
            </>
          )}
        </div>
      </div>

      {/* Feature Modules Grid */}
      <div>
        <h2 style={{ fontSize: '1.5rem', fontWeight: 800, textAlign: 'center', marginBottom: '24px' }}>
          Everything You Need to Excel in College
        </h2>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '20px' }}>
          
          <div className="glass-card glass-card-interactive" style={{ padding: '24px' }}>
            <div style={{ padding: '12px', borderRadius: '12px', background: 'rgba(99, 102, 241, 0.15)', width: 'fit-content', marginBottom: '16px' }}>
              <BrainCircuit style={{ width: '24px', height: '24px', color: '#818cf8' }} />
            </div>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '8px' }}>Context-Aware AI Copilot</h3>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: '1.5' }}>
              Ask questions about your real course schedule, attendance %, exams, and assignments with instant LLM intelligence.
            </p>
          </div>

          <div className="glass-card glass-card-interactive" style={{ padding: '24px' }}>
            <div style={{ padding: '12px', borderRadius: '12px', background: 'rgba(6, 182, 212, 0.15)', width: 'fit-content', marginBottom: '16px' }}>
              <FileText style={{ width: '24px', height: '24px', color: '#06b6d4' }} />
            </div>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '8px' }}>College Document RAG AI</h3>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: '1.5' }}>
              Upload official college PDFs and syllabus rules. Get exact answers backed by page-level source citations.
            </p>
          </div>

          <div className="glass-card glass-card-interactive" style={{ padding: '24px' }}>
            <div style={{ padding: '12px', borderRadius: '12px', background: 'rgba(16, 185, 129, 0.15)', width: 'fit-content', marginBottom: '16px' }}>
              <Calendar style={{ width: '24px', height: '24px', color: '#10b981' }} />
            </div>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '8px' }}>AI Study Planner & Replan</h3>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: '1.5' }}>
              Generate personalized daily study timetables based on exam dates. Hit "Replan" to dynamically adjust missed sessions.
            </p>
          </div>

          <div className="glass-card glass-card-interactive" style={{ padding: '24px' }}>
            <div style={{ padding: '12px', borderRadius: '12px', background: 'rgba(245, 158, 11, 0.15)', width: 'fit-content', marginBottom: '16px' }}>
              <Briefcase style={{ width: '24px', height: '24px', color: '#f59e0b' }} />
            </div>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 700, marginBottom: '8px' }}>Career Resume & Mock Interview</h3>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-muted)', lineHeight: '1.5' }}>
              Analyze your resume against Frontend, Backend, or Data Engineer roles. Practice interactive AI technical interviews.
            </p>
          </div>

        </div>
      </div>

    </div>
  );
}
