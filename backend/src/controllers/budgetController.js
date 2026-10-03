import { getDB, saveDB } from '../storage/db.js';

export const getBudget = (req, res, next) => {
  try {
    const db = getDB();
    const budget = db.budget || { monthlyTarget: 1000, currency: '$', expenses: [] };
    
    const totalSpent = budget.expenses.reduce((sum, exp) => sum + Number(exp.amount || 0), 0);
    const remaining = budget.monthlyTarget - totalSpent;
    const categoryBreakdown = budget.expenses.reduce((acc, curr) => {
      acc[curr.category] = (acc[curr.category] || 0) + Number(curr.amount || 0);
      return acc;
    }, {});

    res.json({
      success: true,
      data: {
        ...budget,
        totalSpent,
        remaining,
        categoryBreakdown
      }
    });
  } catch (error) {
    next(error);
  }
};

export const addExpense = (req, res, next) => {
  try {
    const { title, amount, category, date } = req.body;
    if (!title || !amount) {
      return res.status(400).json({ success: false, error: 'Title and amount are required' });
    }

    const db = getDB();
    if (!db.budget) {
      db.budget = { monthlyTarget: 1200, currency: '$', expenses: [] };
    }

    const newExpense = {
      id: Date.now().toString(),
      title,
      amount: Number(amount),
      category: category || 'General',
      date: date || new Date().toISOString().split('T')[0]
    };

    db.budget.expenses.unshift(newExpense);
    saveDB(db);

    res.status(201).json({ success: true, data: newExpense });
  } catch (error) {
    next(error);
  }
};

export const updateBudgetTarget = (req, res, next) => {
  try {
    const { monthlyTarget } = req.body;
    if (!monthlyTarget || isNaN(monthlyTarget)) {
      return res.status(400).json({ success: false, error: 'Valid monthlyTarget is required' });
    }

    const db = getDB();
    if (!db.budget) db.budget = { monthlyTarget: 1200, currency: '$', expenses: [] };
    db.budget.monthlyTarget = Number(monthlyTarget);

    saveDB(db);
    res.json({ success: true, data: db.budget });
  } catch (error) {
    next(error);
  }
};

export const deleteExpense = (req, res, next) => {
  try {
    const { id } = req.params;
    const db = getDB();
    if (!db.budget) return res.status(404).json({ success: false, error: 'Budget data not found' });

    db.budget.expenses = db.budget.expenses.filter(e => e.id !== id);
    saveDB(db);
    res.json({ success: true, message: 'Expense deleted successfully' });
  } catch (error) {
    next(error);
  }
};
