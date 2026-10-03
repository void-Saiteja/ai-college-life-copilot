import express from 'express';
import { 
  getExams, 
  createExam, 
  updateExam, 
  deleteExam,
  runExamAdvisor 
} from '../controllers/examController.js';
import { authenticateUser } from '../middleware/authMiddleware.js';

const router = express.Router();

router.get('/', authenticateUser, getExams);
router.post('/', authenticateUser, createExam);
router.put('/:id', authenticateUser, updateExam);
router.delete('/:id', authenticateUser, deleteExam);
router.post('/advisor', authenticateUser, runExamAdvisor);

export default router;
