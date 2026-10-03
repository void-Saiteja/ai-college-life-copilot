import express from 'express';
import { getStudyPlan, generateStudyPlan, updateSessionStatus, replanSchedule } from '../controllers/studyPlannerController.js';
import { authenticateUser } from '../middleware/authMiddleware.js';

const router = express.Router();

router.get('/', authenticateUser, getStudyPlan);
router.post('/generate', authenticateUser, generateStudyPlan);
router.put('/session/:id', authenticateUser, updateSessionStatus);
router.post('/replan', authenticateUser, replanSchedule);

export default router;
