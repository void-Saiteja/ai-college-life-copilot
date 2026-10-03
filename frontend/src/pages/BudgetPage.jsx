import React, { useState, useEffect } from 'react';
import BudgetView from '../components/BudgetView';
import api from '../services/api';

export default function BudgetPage() {
  const [budget, setBudget] = useState({
    monthlyTarget: 1200,
    currency: '$',
    totalSpent: 0,
    remaining: 1200,
    expenses: []
  });
  const [loading, setLoading] = useState(true);

  const fetchBudget = async () => {
    try {
      const res = await api.get('/budget');
      if (res.data.success) {
        setBudget(res.data.data);
      }
    } catch (err) {
      console.warn('Budget fallback');
      setBudget({
        monthlyTarget: 1200,
        currency: '$',
        totalSpent: 425.50,
        remaining: 774.50,
        expenses: [
          { id: '1', title: 'Textbooks & Lab Access', amount: 145.50, category: 'Academic', date: new Date().toISOString().split('T')[0] },
          { id: '2', title: 'Campus Meal Pass', amount: 280.00, category: 'Food', date: new Date().toISOString().split('T')[0] }
        ]
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBudget();
  }, []);

  const handleAddExpense = async (newExp) => {
    try {
      const res = await api.post('/budget/expense', newExp);
      if (res.data.success) {
        fetchBudget();
      }
    } catch (err) {
      console.error('Failed to log expense', err);
    }
  };

  const handleDeleteExpense = async (id) => {
    try {
      const res = await api.delete(`/budget/expense/${id}`);
      if (res.data.success) {
        fetchBudget();
      }
    } catch (err) {
      console.error('Failed to delete expense', err);
    }
  };

  const handleUpdateTarget = async (monthlyTarget) => {
    try {
      const res = await api.put('/budget/target', { monthlyTarget });
      if (res.data.success) {
        fetchBudget();
      }
    } catch (err) {
      console.error('Failed to update target', err);
    }
  };

  if (loading) {
    return (
      <div className="glass-card" style={{ padding: '60px', textAlign: 'center', color: 'var(--text-muted)' }}>
        ⏳ Loading your budget and expense records...
      </div>
    );
  }

  return (
    <BudgetView
      budget={budget}
      onAddExpense={handleAddExpense}
      onDeleteExpense={handleDeleteExpense}
      onUpdateTarget={handleUpdateTarget}
    />
  );
}
