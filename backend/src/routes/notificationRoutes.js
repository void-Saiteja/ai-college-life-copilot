import express from 'express';
import { 
  getNotifications, 
  getUnreadCount,
  markAsRead, 
  markAllAsRead,
  deleteNotification,
  generateNotifications
} from '../controllers/notificationController.js';
import { authenticateUser } from '../middleware/authMiddleware.js';

const router = express.Router();

router.use(authenticateUser);

router.get('/', getNotifications);
router.get('/unread-count', getUnreadCount);
router.put('/read-all', markAllAsRead);
router.put('/:id/read', markAsRead);
router.delete('/:id', deleteNotification);
router.post('/generate', generateNotifications);

export default router;
