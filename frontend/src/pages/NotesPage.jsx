import React, { useState, useEffect } from 'react';
import { BookOpen, BrainCircuit } from 'lucide-react';
import NotesView from '../components/NotesView';
import AIStudyAssistantView from '../components/AIStudyAssistantView';
import api from '../services/api';

export default function NotesPage() {
  const [activeTab, setActiveTab] = useState('notes'); // 'notes' | 'ai-lab'
  const [notes, setNotes] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchNotes = async () => {
    try {
      const res = await api.get('/notes');
      if (res.data.success) {
        setNotes(res.data.data || []);
      }
    } catch (err) {
      console.warn('Notes fallback to sample lecture notes');
      setNotes([
        {
          id: '1',
          title: 'Binary Search Trees & Balancing Rules',
          course: 'CS 201',
          content: 'A binary search tree satisfies the BST invariant: left subtree keys <= node key <= right subtree keys. AVL trees maintain balance factor in {-1, 0, 1} through LL, RR, LR, and RL rotations.',
          tags: ['Trees', 'AVL', 'Algorithms'],
          updatedAt: new Date().toISOString()
        },
        {
          id: '2',
          title: 'SQL Indexing & B+ Trees',
          course: 'CS 340',
          content: 'Clustered indices dictate physical row ordering on disk. Non-clustered indices contain pointers to the data pages. B+ tree leaf nodes are linked for high throughput range queries.',
          tags: ['Databases', 'Indexing', 'SQL'],
          updatedAt: new Date().toISOString()
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotes();
  }, []);

  const handleAddNote = async (newNote) => {
    try {
      const res = await api.post('/notes', newNote);
      if (res.data.success) {
        fetchNotes();
      }
    } catch (err) {
      console.error('Failed to create note', err);
    }
  };

  const handleDeleteNote = async (id) => {
    try {
      const res = await api.delete(`/notes/${id}`);
      if (res.data.success) {
        fetchNotes();
      }
    } catch (err) {
      console.error('Failed to delete note', err);
    }
  };

  if (loading) {
    return (
      <div className="glass-card" style={{ padding: '60px', textAlign: 'center', color: 'var(--text-muted)' }}>
        ⏳ Loading your study notes & knowledge records...
      </div>
    );
  }

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Top Tab Bar */}
      <div className="glass-card" style={{ padding: '8px 12px', display: 'flex', gap: '10px' }}>
        <button
          className={`btn ${activeTab === 'notes' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('notes')}
          style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
        >
          <BookOpen style={{ width: '16px', height: '16px' }} />
          <span>Study Notes Library ({notes.length})</span>
        </button>
        <button
          className={`btn ${activeTab === 'ai-lab' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('ai-lab')}
          style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
        >
          <BrainCircuit style={{ width: '16px', height: '16px', color: '#a5b4fc' }} />
          <span>AI Study Lab & Quizzes</span>
        </button>
      </div>

      {/* Main Content */}
      {activeTab === 'notes' ? (
        <NotesView
          notes={notes}
          onAddNote={handleAddNote}
          onDeleteNote={handleDeleteNote}
          setActiveTab={setActiveTab}
        />
      ) : (
        <AIStudyAssistantView
          notes={notes}
        />
      )}
    </div>
  );
}
