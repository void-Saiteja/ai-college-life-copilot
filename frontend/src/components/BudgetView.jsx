import React, { useState } from 'react';
import api from '../services/api';
import { 
  Wallet, 
  Plus, 
  Sparkles, 
  Utensils, 
  Trash2, 
  TrendingDown, 
  PieChart, 
  DollarSign,
  Edit2
} from 'lucide-react';

export default function BudgetView({ budget, onAddExpense, onDeleteExpense, onUpdateTarget }) {
  const [showModal, setShowModal] = useState(false);
  const [showTargetModal, setShowTargetModal] = useState(false);
  const [newTarget, setNewTarget] = useState('');
  const [expenseTitle, setExpenseTitle] = useState('');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('Food');
  
  const [aiMealPlan, setAiMealPlan] = useState(null);
  const [isGeneratingMeal, setIsGeneratingMeal] = useState(false);

  const totalSpent = budget.totalSpent || 0;
  const target = budget.monthlyTarget || 1200;
  const remaining = target - totalSpent;
  const percentage = Math.min(100, Math.round((totalSpent / target) * 100));

  const handleAddSubmit = (e) => {
    e.preventDefault();
    if (!expenseTitle || !amount) return;
    onAddExpense({ title: expenseTitle, amount: Number(amount), category });
    setExpenseTitle('');
    setAmount('');
    setShowModal(false);
  };

  const handleTargetSubmit = (e) => {
    e.preventDefault();
    if (!newTarget || isNaN(newTarget)) return;
    if (onUpdateTarget) onUpdateTarget(Number(newTarget));
    setShowTargetModal(false);
  };

  const handleGetMealPlan = async () => {
    setIsGeneratingMeal(true);
    try {
      const res = await api.post('/ai/meal-planner', { budgetRemaining: remaining });
      if (res.data.success) {
        setAiMealPlan(res.data.data);
      }
    } catch (err) {
      console.error('Meal planner error', err);
    } finally {
      setIsGeneratingMeal(false);
    }
  };

  return (
    <div className="animate-fade-in" style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      
      {/* Header bar */}
      <div className="glass-card" style={{ padding: '20px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.4rem', fontWeight: 800, display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Wallet style={{ width: '22px', height: '22px', color: '#06b6d4' }} /> Student Budget & Meal Copilot
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem', marginTop: '2px' }}>
            Track expenses, manage allowance, and discover cheap student meals.
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button className="btn btn-secondary" onClick={handleGetMealPlan} disabled={isGeneratingMeal}>
            <Utensils style={{ width: '16px', height: '16px', color: '#10b981' }} /> {isGeneratingMeal ? 'Planning Meals...' : 'AI Cheap Meal Planner'}
          </button>
          <button className="btn btn-primary" onClick={() => setShowModal(true)}>
            <Plus style={{ width: '16px', height: '16px' }} /> Log Expense
          </button>
        </div>
      </div>

      {/* Budget Summary Metrics */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '16px' }}>
        
        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <span style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Monthly Target Budget</span>
            <button 
              className="btn btn-secondary btn-sm" 
              style={{ padding: '2px 8px', fontSize: '0.72rem' }}
              onClick={() => { setNewTarget(target.toString()); setShowTargetModal(true); }}
            >
              <Edit2 style={{ width: '12px', height: '12px' }} /> Edit
            </button>
          </div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: '#f8fafc', marginTop: '4px' }}>
            ${target.toFixed(2)}
          </div>
        </div>

        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Total Spent</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: percentage > 85 ? '#f43f5e' : '#67e8f9', marginTop: '4px' }}>
            ${totalSpent.toFixed(2)}
          </div>
        </div>

        <div className="glass-card" style={{ padding: '20px' }}>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>Remaining Allowance</div>
          <div style={{ fontSize: '1.8rem', fontWeight: 800, color: remaining < 0 ? '#f43f5e' : '#6ee7b7', marginTop: '4px' }}>
            ${remaining.toFixed(2)}
          </div>
        </div>

      </div>

      {/* Progress Bar */}
      <div className="glass-card" style={{ padding: '20px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', fontSize: '0.88rem' }}>
          <span>Budget Usage: <strong>{percentage}%</strong></span>
          <span style={{ color: 'var(--text-muted)' }}>Target: ${target}</span>
        </div>
        <div style={{ width: '100%', height: '10px', background: 'rgba(255, 255, 255, 0.08)', borderRadius: '6px', overflow: 'hidden' }}>
          <div style={{ width: `${percentage}%`, height: '100%', background: percentage > 90 ? '#f43f5e' : 'linear-gradient(90deg, #06b6d4, #10b981)', borderRadius: '6px', transition: 'width 0.4s ease' }} />
        </div>
      </div>

      {/* AI Cheap Meal Plan Results */}
      {aiMealPlan && (
        <div className="glass-card" style={{ padding: '20px', background: 'linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(15, 23, 42, 0.95))', border: '1px solid rgba(16, 185, 129, 0.3)' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 700, color: '#6ee7b7', display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
            <Utensils style={{ width: '18px', height: '18px' }} /> AI College Meal & Budget Recommendations
          </h3>
          <p style={{ fontSize: '0.9rem', color: '#e2e8f0', marginBottom: '14px' }}>{aiMealPlan.budgetAdvice}</p>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '12px' }}>
            {aiMealPlan.suggestedMeals.map((meal, idx) => (
              <div key={idx} style={{ padding: '14px', borderRadius: '8px', background: 'rgba(255, 255, 255, 0.04)', border: '1px solid var(--border-color)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 700, fontSize: '0.92rem', marginBottom: '4px' }}>
                  <span>{meal.name}</span>
                  <span style={{ color: '#6ee7b7' }}>{meal.cost}</span>
                </div>
                <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)' }}>⏱️ {meal.time} prepare time</div>
                <p style={{ fontSize: '0.78rem', color: '#cbd5e1', marginTop: '6px' }}>{meal.instructions}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Expense History Table */}
      <div className="glass-card" style={{ padding: '24px' }}>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '16px' }}>Recent Expenses</h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {budget.expenses?.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '20px' }}>No logged expenses yet.</p>
          ) : (
            budget.expenses?.map(item => (
              <div key={item.id} style={{
                padding: '14px 16px',
                borderRadius: 'var(--radius-sm)',
                background: 'rgba(255, 255, 255, 0.03)',
                border: '1px solid var(--border-color)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between'
              }}>
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.95rem' }}>{item.title}</div>
                  <div style={{ fontSize: '0.78rem', color: 'var(--text-muted)', marginTop: '2px' }}>
                    🏷️ {item.category} • 📅 {item.date}
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
                  <span style={{ fontSize: '1.05rem', fontWeight: 800, color: '#fca5a5' }}>
                    -${item.amount.toFixed(2)}
                  </span>
                  <button className="btn btn-secondary btn-sm" onClick={() => onDeleteExpense(item.id)}>
                    <Trash2 style={{ width: '14px', height: '14px' }} />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Modal to Log Expense */}
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
          <div className="glass-card" style={{ width: '100%', maxWidth: '450px', padding: '28px' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, marginBottom: '16px' }}>Log New Expense</h3>

            <form onSubmit={handleAddSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Title / Item Name</label>
                <input type="text" className="input-field" required placeholder="e.g. Textbooks or Campus Lunch" value={expenseTitle} onChange={(e) => setExpenseTitle(e.target.value)} />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Amount ($)</label>
                  <input type="number" step="0.01" className="input-field" required placeholder="14.50" value={amount} onChange={(e) => setAmount(e.target.value)} />
                </div>
                <div>
                  <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Category</label>
                  <select className="input-field" value={category} onChange={(e) => setCategory(e.target.value)}>
                    <option value="Food">Food & Dining</option>
                    <option value="Academic">Academic / Books</option>
                    <option value="Transport">Transport</option>
                    <option value="Rent">Rent & Housing</option>
                    <option value="Entertainment">Entertainment</option>
                  </select>
                </div>
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '12px' }}>
                <button type="button" className="btn btn-secondary" style={{ width: '50%' }} onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" style={{ width: '50%' }}>
                  Add Expense
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal to Update Monthly Target */}
      {showTargetModal && (
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
          <div className="glass-card" style={{ width: '100%', maxWidth: '420px', padding: '28px' }}>
            <h3 style={{ fontSize: '1.2rem', fontWeight: 800, marginBottom: '8px' }}>Update Monthly Budget</h3>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-muted)', marginBottom: '16px' }}>
              Set your target monthly allowance to recalibrate your budget usage & meal plans.
            </p>

            <form onSubmit={handleTargetSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'block', marginBottom: '4px' }}>Target Budget ($)</label>
                <input 
                  type="number" 
                  step="10" 
                  className="input-field" 
                  required 
                  placeholder="1200" 
                  value={newTarget} 
                  onChange={(e) => setNewTarget(e.target.value)} 
                />
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
                <button type="button" className="btn btn-secondary" style={{ width: '50%' }} onClick={() => setShowTargetModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" style={{ width: '50%' }}>
                  Save Target
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}
