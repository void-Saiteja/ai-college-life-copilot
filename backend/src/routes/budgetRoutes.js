import express from 'express';
import { getBudget, addExpense, updateBudgetTarget, deleteExpense } from '../controllers/budgetController.js';

import { authenticateUser } from '../middleware/authMiddleware.js';

const router = express.Router();

router.use(authenticateUser);

router.get('/', getBudget);
router.post('/expense', addExpense);
router.put('/target', updateBudgetTarget);
router.delete('/expense/:id', deleteExpense);

export default router;
