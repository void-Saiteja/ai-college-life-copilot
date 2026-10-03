import { getDB, saveDB } from '../storage/db.js';

function getStudentBudget(db, studentKey) {
  if (!db.budgets || typeof db.budgets !== 'object') {
    db.budgets = {};
  }
  if (!db.budgets[studentKey]) {
    // Legacy seed data only available to default student std-1
    if (studentKey === 'std-1' && db.budget) {
      db.budgets[studentKey] = JSON.parse(JSON.stringify(db.budget));
    } else {
      db.budgets[studentKey] = { monthlyTarget: 1200, currency: '$', expenses: [] };
    }
  }
  return db.budgets[studentKey];
}

export const getBudget = (req, res, next) => {
  try {
    const studentKey = req.user?.studentId || req.user?.id;
    if (!studentKey) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();
    const budget = getStudentBudget(db, studentKey);
    
    const totalSpent = (budget.expenses || []).reduce((sum, exp) => sum + Number(exp.amount || 0), 0);
    const remaining = budget.monthlyTarget - totalSpent;
    const categoryBreakdown = (budget.expenses || []).reduce((acc, curr) => {
      const cat = curr.category || 'General';
      acc[cat] = (acc[cat] || 0) + Number(curr.amount || 0);
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
    const studentKey = req.user?.studentId || req.user?.id;
    if (!studentKey) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { title, amount, category, date } = req.body;

    // Strict validation
    if (!title || typeof title !== 'string' || title.trim().length === 0 || title.length > 150) {
      return res.status(400).json({ success: false, error: 'Title is required (1-150 characters)' });
    }

    const numAmount = Number(amount);
    if (!isFinite(numAmount) || numAmount <= 0 || numAmount > 100000) {
      return res.status(400).json({
        success: false,
        error: 'Amount must be a positive number up to $100,000'
      });
    }

    if (category && (typeof category !== 'string' || category.length > 50)) {
      return res.status(400).json({ success: false, error: 'Category must be a string up to 50 characters' });
    }

    let parsedDate = new Date().toISOString().split('T')[0];
    if (date) {
      const d = new Date(date);
      if (!isNaN(d.getTime())) {
        parsedDate = d.toISOString().split('T')[0];
      }
    }

    const db = getDB();
    const budget = getStudentBudget(db, studentKey);

    const newExpense = {
      id: 'exp-' + Date.now() + '-' + Math.random().toString(36).substring(2, 6),
      student_id: studentKey,
      title: title.trim(),
      amount: Math.round(numAmount * 100) / 100,
      category: category ? category.trim() : 'General',
      date: parsedDate
    };

    budget.expenses.unshift(newExpense);
    saveDB(db);

    res.status(201).json({ success: true, data: newExpense });
  } catch (error) {
    next(error);
  }
};

export const updateBudgetTarget = (req, res, next) => {
  try {
    const studentKey = req.user?.studentId || req.user?.id;
    if (!studentKey) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { monthlyTarget } = req.body;
    const numTarget = Number(monthlyTarget);

    if (!isFinite(numTarget) || numTarget <= 0 || numTarget > 1000000) {
      return res.status(400).json({
        success: false,
        error: 'Valid monthlyTarget must be a positive number up to $1,000,000'
      });
    }

    const db = getDB();
    const budget = getStudentBudget(db, studentKey);
    budget.monthlyTarget = Math.round(numTarget * 100) / 100;

    saveDB(db);
    res.json({ success: true, data: budget });
  } catch (error) {
    next(error);
  }
};

export const deleteExpense = (req, res, next) => {
  try {
    const studentKey = req.user?.studentId || req.user?.id;
    if (!studentKey) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();
    const budget = getStudentBudget(db, studentKey);

    const expenseIndex = (budget.expenses || []).findIndex(e => e.id === id);
    if (expenseIndex === -1) {
      return res.status(404).json({ success: false, error: 'Expense not found in your budget' });
    }

    budget.expenses.splice(expenseIndex, 1);
    saveDB(db);
    res.json({ success: true, message: 'Expense deleted successfully' });
  } catch (error) {
    next(error);
  }
};
