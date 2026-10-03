import express from 'express';
import { getChatSessions, getSessionMessages, postChatMessage, deleteChatSession } from '../controllers/aiCopilotController.js';
import { authenticateUser } from '../middleware/authMiddleware.js';

const router = express.Router();

router.get('/sessions', authenticateUser, getChatSessions);
router.get('/sessions/:id', authenticateUser, getSessionMessages);
router.post('/chat', authenticateUser, postChatMessage);
router.delete('/sessions/:id', authenticateUser, deleteChatSession);

export default router;
