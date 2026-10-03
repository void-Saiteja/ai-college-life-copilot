import express from 'express';
import { summarizeNotes, prioritizeTasks, chatCopilot, mealAndBudgetPlanner } from '../controllers/aiController.js';
import { authenticateUser } from '../middleware/authMiddleware.js';

const router = express.Router();

// Strict Authentication on all AI endpoints
router.use(authenticateUser);

router.post('/summarize', summarizeNotes);
router.post('/prioritize', prioritizeTasks);
router.post('/chat', chatCopilot);
router.post('/meal-planner', mealAndBudgetPlanner);

export default router;
