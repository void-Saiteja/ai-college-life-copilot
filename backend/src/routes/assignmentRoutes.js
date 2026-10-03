import express from 'express';
import { 
  getAssignments, 
  createAssignment, 
  updateAssignment, 
  deleteAssignment,
  runAssignmentAdvisor 
} from '../controllers/assignmentController.js';
import { authenticateUser } from '../middleware/authMiddleware.js';

const router = express.Router();

router.get('/', authenticateUser, getAssignments);
router.post('/', authenticateUser, createAssignment);
router.put('/:id', authenticateUser, updateAssignment);
router.delete('/:id', authenticateUser, deleteAssignment);
router.post('/advisor', authenticateUser, runAssignmentAdvisor);

export default router;
