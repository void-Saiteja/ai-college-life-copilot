import React, { useState, useEffect } from 'react';
import { 
  BrainCircuit, 
  Send, 
  Plus, 
  Trash2, 
  MessageSquare, 
  Sparkles, 
  BookOpen,
  FileText
} from 'lucide-react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function AICopilotPage() {
  const { user } = useAuth();
  
  const [sessions, setSessions] = useState([]);
  const [activeSessionId, setActiveSessionId] = useState(null);
  const [messages, setMessages] = useState([
    { id: '1', sender: 'assistant', content: `👋 Hello ${user?.name || 'Alex'}! I am your AI College Copilot. I have synced your academic profile, attendance, and upcoming exams, as well as official college policy documents. What would you like to check today?` }
  ]);

  const [inputMsg, setInputMsg] = useState('');
  const [loading, setLoading] = useState(false);

  const fetchSessions = async () => {
    try {
      const res = await api.get('/chat/sessions');
      if (res.data.success) {
        setSessions(res.data.data);
      }
    } catch (err) {
      console.warn('Sessions load notice: running in session mode.');
    }
  };

  useEffect(() => {
    fetchSessions();
  }, []);

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!inputMsg.trim()) return;

    const userText = inputMsg;
    const tempUserMsg = { id: Date.now().toString(), sender: 'user', content: userText };
    
    setMessages(prev => [...prev, tempUserMsg]);
    setInputMsg('');
    setLoading(true);

    try {
      const res = await api.post('/chat/chat', {
        sessionId: activeSessionId,
        message: userText
      });

      if (res.data.success) {
        const { sessionId, assistantMessage } = res.data.data;
        if (!activeSessionId) {
          setActiveSessionId(sessionId);
          fetchSessions();
        }
        setMessages(prev => [...prev, assistantMessage]);
      }
    } catch (err) {
      setMessages(prev => [...prev, {
        id: Date.now().toString(),
        sender: 'assistant',
        content: '⚠️ I encountered a temporary connection issue. Please check your network or try asking again.'
      }]);
    } finally {
      setLoading(false);
    }
  };

  const handleNewChat = () => {
    setActiveSessionId(null);
    setMessages([
      { id: '1', sender: 'assistant', content: `👋 Starting a fresh conversation! Ask me about your assignments, attendance, or official college handbook policies.` }
    ]);
  };

  const handleDeleteSession = async (id) => {
    try {
      await api.delete(`/chat/sessions/${id}`);
      if (activeSessionId === id) handleNewChat();
      fetchSessions();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="animate-fade-in" style={{ display: 'grid', gridTemplateColumns: '260px 1fr', gap: '20px', height: 'calc(100vh - 120px)' }}>
      
      {/* Sessions Sidebar */}
      <div className="glass-card" style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
        <button className="btn btn-primary" onClick={handleNewChat} style={{ width: '100%' }}>
          <Plus style={{ width: '16px', height: '16px' }} /> New Conversation
        </button>

        <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--text-dim)', textTransform: 'uppercase', marginTop: '8px' }}>
          Conversation History
        </div>

        <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '6px' }}>
          {sessions.length === 0 ? (
            <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', padding: '10px 0' }}>No previous conversations saved.</p>
          ) : (
            sessions.map(s => (
              <div
                key={s.id}
                onClick={() => setActiveSessionId(s.id)}
                style={{
                  padding: '10px 12px',
                  borderRadius: '8px',
                  background: activeSessionId === s.id ? 'rgba(99, 102, 241, 0.2)' : 'rgba(255, 255, 255, 0.03)',
                  border: activeSessionId === s.id ? '1px solid #6366f1' : '1px solid transparent',
                  cursor: 'pointer',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center'
                }}
              >
                <div style={{ fontSize: '0.85rem', fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', color: '#fff' }}>
                  {s.title}
                </div>
                <button className="btn btn-secondary btn-sm" style={{ padding: '2px 4px' }} onClick={(e) => { e.stopPropagation(); handleDeleteSession(s.id); }}>
                  <Trash2 style={{ width: '12px', height: '12px', color: '#fca5a5' }} />
                </button>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Main Chat Window */}
      <div className="glass-card" style={{ padding: '24px', display: 'flex', flexDirection: 'column', height: '100%' }}>
        
        {/* Academic Context Header Pill */}
        <div style={{ padding: '10px 16px', borderRadius: '10px', background: 'rgba(99, 102, 241, 0.1)', border: '1px solid rgba(99, 102, 241, 0.2)', marginBottom: '16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <BrainCircuit style={{ width: '18px', height: '18px', color: '#818cf8' }} />
            <span style={{ fontSize: '0.88rem', fontWeight: 700, color: '#c7d2fe' }}>AI Copilot (Synced with Academic Records & RAG Documents)</span>
          </div>
          <span className="badge badge-indigo">
            <Sparkles style={{ width: '12px', height: '12px' }} /> Student & RAG Context Active
          </span>
        </div>

        {/* Message Trajectory */}
        <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '14px', paddingRight: '6px', marginBottom: '16px' }}>
          {messages.map(msg => (
            <div
              key={msg.id}
              style={{
                alignSelf: msg.sender === 'user' ? 'flex-end' : 'flex-start',
                maxWidth: '82%',
                padding: '14px 18px',
                borderRadius: msg.sender === 'user' ? '18px 18px 2px 18px' : '18px 18px 18px 2px',
                background: msg.sender === 'user' ? 'linear-gradient(135deg, #6366f1, #4f46e5)' : 'rgba(255, 255, 255, 0.05)',
                border: msg.sender === 'user' ? 'none' : '1px solid var(--border-color)',
                color: '#fff',
                fontSize: '0.93rem',
                lineHeight: '1.5',
                whiteSpace: 'pre-line'
              }}
            >
              <div>{msg.content}</div>

              {msg.sources && msg.sources.length > 0 && (
                <div style={{ marginTop: '12px', paddingTop: '10px', borderTop: '1px solid rgba(255, 255, 255, 0.12)', fontSize: '0.8rem', color: '#c7d2fe' }}>
                  <div style={{ fontWeight: 700, marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <FileText style={{ width: '14px', height: '14px', color: '#67e8f9' }} /> Document Sources:
                  </div>
                  {msg.sources.map((s, idx) => (
                    <div key={idx} style={{ color: 'var(--text-muted)' }}>
                      • <strong>{s.documentName}</strong> — Page {s.pageNumber}
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
          {loading && (
            <div style={{ alignSelf: 'flex-start', padding: '10px 16px', borderRadius: '12px', background: 'rgba(255, 255, 255, 0.05)', color: 'var(--text-muted)', fontSize: '0.85rem' }}>
              Thinking, searching semantic vector documents, and analyzing academic context...
            </div>
          )}
        </div>

        {/* Message Input Bar */}
        <form onSubmit={handleSendMessage} style={{ display: 'flex', gap: '12px' }}>
          <input 
            type="text" 
            className="input-field" 
            placeholder="Ask about your attendance, exam schedule, homework due dates, or college policy handbook rules..." 
            value={inputMsg}
            onChange={(e) => setInputMsg(e.target.value)}
            disabled={loading}
          />
          <button type="submit" className="btn btn-primary" disabled={loading || !inputMsg.trim()}>
            <Send style={{ width: '16px', height: '16px' }} /> Send
          </button>
        </form>

      </div>

    </div>
  );
}
