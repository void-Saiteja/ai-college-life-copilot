import express from 'express';
import { summarizeNotes, prioritizeTasks, chatCopilot, mealAndBudgetPlanner } from '../controllers/aiController.js';

const router = express.Router();

router.post('/summarize', summarizeNotes);
router.post('/prioritize', prioritizeTasks);
router.post('/chat', chatCopilot);
router.post('/meal-planner', mealAndBudgetPlanner);

export default router;
