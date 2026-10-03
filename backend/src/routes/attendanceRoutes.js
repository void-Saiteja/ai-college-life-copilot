import express from 'express';
import { 
  getAttendance, 
  updateAttendance, 
  calculateProjection,
  explainAttendance 
} from '../controllers/attendanceController.js';
import { authenticateUser } from '../middleware/authMiddleware.js';

const router = express.Router();

router.get('/', authenticateUser, getAttendance);
router.put('/:id', authenticateUser, updateAttendance);
router.post('/projection', authenticateUser, calculateProjection);
router.post('/explain', authenticateUser, explainAttendance);

export default router;
