import React, { useState } from 'react';
import { 
  BookOpen, 
  Plus, 
  Sparkles, 
  Trash2, 
  Search, 
  Tag, 
  BrainCircuit,
  ArrowRight
} from 'lucide-react';

export default function NotesView({ notes, onAddNote, onDeleteNote, setActiveTab }) {
  const [showModal, setShowModal] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  
  // New note form
  const [title, setTitle] = useState('');
  const [course, setCourse] = useState('');
  const [content, setContent] = useState('');
  const [tags, setTags] = useState('');

  const filteredNotes = notes.filter(n => 
    n.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
    n.course.toLowerCase().includes(searchQuery.toLowerCase()) ||
    n.content.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!title || !content) return;
    onAddNote({ title, course, content, tags });
    setTitle('');
    setCourse('');
    setContent('');
    setTags('');
    setShowModal(false);
  };

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Header bar */}
      <div className="glass-card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <BookOpen style={{ width: '22px', height: '22px', color: '#a78bfa' }} /> Study Notes & Knowledge Hub
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '2px' }}>
            Store lecture materials, formula sheets, and convert notes into AI quizzes.
          </p>
        </div>

        <button className="btn btn-primary" onClick={() => setShowModal(true)}>
          <Plus style={{ width: '16px', height: '16px' }} /> Create Study Note
        </button>
      </div>

      {/* Search Input */}
      <div style={{ position: 'relative' }}>
        <Search style={{ position: 'absolute', left: '14px', top: '50%', transform: 'translateY(-50%)', width: '18px', height: '18px', color: 'var(--text-dim)' }} />
        <input 
          type="text" 
          className="input-field" 
          style={{ paddingLeft: '42px' }}
          placeholder="Search study notes by topic, keyword, or course code..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
        />
      </div>

      {/* Notes Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '16px' }}>
        {filteredNotes.length === 0 ? (
          <div className="glass-card" style={{ gridColumn: '1 / -1', padding: '40px', textAlign: 'center', color: 'var(--text-muted)' }}>
            No study notes matching your query.
          </div>
        ) : (
          filteredNotes.map(item => (
            <div key={item.id} className="glass-card glass-card-interactive" style={{ padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <span className="badge badge-indigo" style={{ marginBottom: '6px' }}>{item.course}</span>
                  <h3 style={{ fontSize: '1.05rem', fontWeight: 700 }}>{item.title}</h3>
                </div>
                <button className="btn btn-secondary btn-sm" onClick={() => onDeleteNote(item.id)} style={{ color: '#fca5a5' }}>
                  <Trash2 style={{ width: '14px', height: '14px' }} />
                </button>
              </div>

              <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', display: '-webkit-box', WebkitLineClamp: 4, WebkitBoxOrient: 'vertical', overflow: 'hidden', lineHeight: '1.5' }}>
                {item.content}
              </p>

              {item.tags?.length > 0 && (
                <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                  {item.tags.map((tag, idx) => (
                    <span key={idx} style={{ fontSize: '0.72rem', background: 'rgba(255, 255, 255, 0.05)', color: '#c7d2fe', padding: '2px 8px', borderRadius: '4px' }}>
                      #{tag}
                    </span>
                  ))}
                </div>
              )}

              <div style={{ marginTop: 'auto', paddingTop: '10px', borderTop: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '0.72rem', color: 'var(--text-dim)' }}>
                  Updated {new Date(item.updatedAt || Date.now()).toLocaleDateString()}
                </span>
                <button className="btn btn-emerald btn-sm" onClick={() => setActiveTab('ai-lab')}>
                  <BrainCircuit style={{ width: '12px', height: '12px' }} /> AI Quiz <ArrowRight style={{ width: '12px', height: '12px' }} />
                </button>
              </div>
            </div>
          ))
        )}
      </div>

      {/* Modal to Create Note */}
      {showModal && (
        <div style={{
          position: 'fixed',
          top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0, 0, 0, 0.75)',
          backdropFilter: 'blur(8px)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 100
        }}>
          <div className="glass-card" style={{ width: '100%', maxWidth: '550px', padding: '28px' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, marginBottom: '16px' }}>Create Study Note</h3>

            <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Note Title / Topic</label>
                <input type="text" className="input-field" required placeholder="e.g. Graph Traversal Algorithms" value={title} onChange={(e) => setTitle(e.target.value)} />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Course Code</label>
                  <input type="text" className="input-field" placeholder="e.g. CS 201" value={course} onChange={(e) => setCourse(e.target.value)} />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Tags (comma separated)</label>
                  <input type="text" className="input-field" placeholder="BFS, DFS, Graphs" value={tags} onChange={(e) => setTags(e.target.value)} />
                </div>
              </div>

              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Note Content</label>
                <textarea className="input-field textarea-field" rows={6} required placeholder="Enter key formulas, lecture points, or textbook definitions..." value={content} onChange={(e) => setContent(e.target.value)} />
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '12px' }}>
                <button type="button" className="btn btn-secondary" style={{ width: '50%' }} onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" style={{ width: '50%' }}>
                  Save Note
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
