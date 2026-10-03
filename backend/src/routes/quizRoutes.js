import express from 'express';
import { 
  generateQuiz, 
  submitQuiz, 
  getQuizzes, 
  getQuizById,
  deleteQuiz,
  runQuizAdvisor 
} from '../controllers/quizController.js';
import { authenticateUser } from '../middleware/authMiddleware.js';

const router = express.Router();

router.post('/generate', authenticateUser, generateQuiz);
router.get('/', authenticateUser, getQuizzes);
router.get('/history', authenticateUser, getQuizzes);
router.get('/:id', authenticateUser, getQuizById);
router.post('/:id/submit', authenticateUser, submitQuiz);
router.post('/submit', authenticateUser, submitQuiz); // Backward compatibility
router.delete('/:id', authenticateUser, deleteQuiz);
router.post('/advisor', authenticateUser, runQuizAdvisor);

export default router;
