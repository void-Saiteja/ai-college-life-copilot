import express from 'express';
import { getSchedule, addScheduleItem, deleteScheduleItem } from '../controllers/scheduleController.js';

import { authenticateUser } from '../middleware/authMiddleware.js';

const router = express.Router();

router.use(authenticateUser);

router.get('/', getSchedule);
router.post('/', addScheduleItem);
router.delete('/:id', deleteScheduleItem);

export default router;
