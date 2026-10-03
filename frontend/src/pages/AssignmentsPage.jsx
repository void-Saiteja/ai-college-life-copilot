import React, { useState, useEffect } from 'react';
import AssignmentsView from '../components/AssignmentsView';
import api from '../services/api';

export default function AssignmentsPage() {
  const [assignments, setAssignments] = useState([]);
  const [summary, setSummary] = useState({ total: 0, pending: 0, completed: 0, overdue: 0, dueSoon: 0 });
  const [loading, setLoading] = useState(true);

  const fetchAssignments = async (filterStatus = 'all') => {
    try {
      const res = await api.get(`/assignments?status=${filterStatus}`);
      if (res.data.success) {
        setAssignments(res.data.data || []);
        if (res.data.summary) {
          setSummary(res.data.summary);
        }
      }
    } catch (err) {
      console.warn('Assignments fetch fallback');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAssignments();
  }, []);

  const handleAddAssignment = async (newObj) => {
    try {
      const res = await api.post('/assignments', newObj);
      if (res.data.success) {
        fetchAssignments();
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleUpdateAssignment = async (id, updateFields) => {
    try {
      const res = await api.put(`/assignments/${id}`, updateFields);
      if (res.data.success) {
        fetchAssignments();
      }
    } catch (err) {
      console.error(err);
    }
  };

  const handleDeleteAssignment = async (id) => {
    try {
      const res = await api.delete(`/assignments/${id}`);
      if (res.data.success) {
        fetchAssignments();
      }
    } catch (err) {
      console.error(err);
    }
  };

  if (loading) return <div className="glass-card" style={{ padding: '60px', textAlign: 'center' }}>⏳ Loading Assignments & Deadline Intelligence...</div>;

  return (
    <AssignmentsView
      assignments={assignments}
      summary={summary}
      onAddAssignment={handleAddAssignment}
      onUpdateAssignment={handleUpdateAssignment}
      onDeleteAssignment={handleDeleteAssignment}
      onRefresh={fetchAssignments}
    />
  );
}
