import express from 'express';
import { getDashboardStats } from '../controllers/studentController.js';
import { authenticateUser } from '../middleware/authMiddleware.js';

const router = express.Router();

router.get('/dashboard', authenticateUser, getDashboardStats);

export default router;
