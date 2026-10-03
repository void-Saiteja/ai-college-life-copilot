import React, { useState, useEffect } from 'react';
import { FileText, Search, Upload, Trash2, Sparkles, BookOpen, ShieldAlert } from 'lucide-react';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function DocumentsPage() {
  const { user } = useAuth();
  
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [ragResult, setRagResult] = useState(null);
  const [searching, setSearching] = useState(false);

  // Upload modal state
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [docTitle, setDocTitle] = useState('');
  const [docText, setDocText] = useState('');

  const fetchDocs = async () => {
    try {
      const res = await api.get('/documents');
      if (res.data.success) {
        setDocuments(res.data.data);
      }
    } catch (err) {
      console.warn('Docs fetch, using local state fallback.');
      setDocuments([
        { id: 'doc-1', title: 'College Academic Rules & Attendance Policy 2026.pdf', file_name: 'Academic_Policy_2026.pdf', file_size: 142850, chunk_count: 3 }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDocs();
  }, []);

  const handleRAGSearch = async (e) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;

    setSearching(true);
    setRagResult(null);

    try {
      const res = await api.post('/documents/query', { query: searchQuery });
      if (res.data.success) {
        setRagResult(res.data.data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setSearching(false);
    }
  };

  const handleUploadSubmit = async (e) => {
    e.preventDefault();
    try {
      await api.post('/documents/upload', { title: docTitle, text: docText });
      setDocTitle(''); setDocText('');
      setShowUploadModal(false);
      fetchDocs();
    } catch (err) {
      console.error(err);
    }
  };

  const handleDeleteDoc = async (id) => {
    try {
      await api.delete(`/documents/${id}`);
      fetchDocs();
    } catch (err) {
      console.error(err);
    }
  };

  if (loading) return <div className="glass-card" style={{ padding: '60px', textAlign: 'center' }}>⏳ Loading Document Repository...</div>;

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Header bar */}
      <div className="glass-card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <FileText style={{ width: '22px', height: '22px', color: '#06b6d4' }} /> RAG College Document Knowledge Assistant
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '2px' }}>
            Query official college policy PDFs with source citation attribution.
          </p>
        </div>

        {user?.role === 'ADMIN' && (
          <button className="btn btn-primary" onClick={() => setShowUploadModal(true)}>
            <Upload style={{ width: '16px', height: '16px' }} /> Upload Official PDF / Text
          </button>
        )}
      </div>

      {/* RAG Search Bar */}
      <div className="glass-card" style={{ padding: '24px' }}>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '12px' }}>Ask Question About College Documents</h3>

        <form onSubmit={handleRAGSearch} style={{ display: 'flex', gap: '12px' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <Search style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', width: '18px', height: '18px', color: 'var(--text-dim)' }} />
            <input 
              type="text" 
              className="input-field" 
              style={{ paddingLeft: '42px' }}
              placeholder="e.g. What is the minimum attendance requirement for final exams?"
              value={searchQuery}
              onChange={e => setSearchQuery(e.target.value)}
            />
          </div>
          <button type="submit" className="btn btn-primary" disabled={searching || !searchQuery.trim()}>
            {searching ? 'Retrieving Chunks...' : 'Query RAG AI'}
          </button>
        </form>

        {/* RAG Search Answer Result */}
        {ragResult && (
          <div style={{ marginTop: '20px', padding: '20px', borderRadius: '12px', background: ragResult.found ? 'rgba(6, 182, 212, 0.08)' : 'rgba(244, 63, 94, 0.08)', border: ragResult.found ? '1px solid rgba(6, 182, 212, 0.25)' : '1px solid rgba(244, 63, 94, 0.25)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
              <Sparkles style={{ width: '18px', height: '18px', color: ragResult.found ? '#67e8f9' : '#fca5a5' }} />
              <h4 style={{ fontSize: '1rem', fontWeight: 700, color: ragResult.found ? '#67e8f9' : '#fca5a5' }}>
                {ragResult.found ? 'Answer from Official College Document' : 'Information Not Found'}
              </h4>
            </div>

            <p style={{ fontSize: '0.92rem', lineHeight: '1.6', whiteSpace: 'pre-line', color: '#f8fafc' }}>
              {ragResult.answer}
            </p>

            {ragResult.sources?.length > 0 && (
              <div style={{ marginTop: '14px', paddingTop: '12px', borderTop: '1px solid var(--border-color)' }}>
                <div style={{ fontSize: '0.78rem', color: '#c7d2fe', fontWeight: 700, marginBottom: '6px' }}>
                  📑 Source Citations:
                </div>
                {ragResult.sources.map((s, idx) => (
                  <div key={idx} style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>
                    • <strong>{s.documentTitle}</strong> (Page {s.pageNumber})
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Uploaded Documents Table */}
      <div className="glass-card" style={{ padding: '24px' }}>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '16px' }}>Indexed Document Repository</h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {documents.map((doc) => (
            <div key={doc.id} style={{ padding: '14px 16px', borderRadius: '8px', background: 'rgba(255, 255, 255, 0.03)', border: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <FileText style={{ width: '20px', height: '20px', color: '#67e8f9' }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{doc.title}</div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                    {doc.file_name} • {(doc.file_size / 1024).toFixed(1)} KB • {doc.chunk_count} RAG Chunks Indexed
                  </div>
                </div>
              </div>

              {user?.role === 'ADMIN' && (
                <button className="btn btn-secondary btn-sm" onClick={() => handleDeleteDoc(doc.id)}>
                  <Trash2 style={{ width: '14px', height: '14px', color: '#fca5a5' }} />
                </button>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Upload Modal */}
      {showUploadModal && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.75)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100 }}>
          <div className="glass-card" style={{ maxWidth: '500px', width: '100%', padding: '28px' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, marginBottom: '16px' }}>Upload College Document</h3>
            <form onSubmit={handleUploadSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Document Title</label>
                <input type="text" className="input-field" required placeholder="e.g. Examination Guidelines 2026.pdf" value={docTitle} onChange={e=>setDocTitle(e.target.value)} />
              </div>
              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Document Text / Rules Extract</label>
                <textarea className="input-field textarea-field" rows={6} required placeholder="Paste official PDF text content to chunk for vector search..." value={docText} onChange={e=>setDocText(e.target.value)} />
              </div>
              <div style={{ display: 'flex', gap: '10px', marginTop: '8px' }}>
                <button type="button" className="btn btn-secondary" style={{ width: '50%' }} onClick={() => setShowUploadModal(false)}>Cancel</button>
                <button type="submit" className="btn btn-primary" style={{ width: '50%' }}>Upload & Index</button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
