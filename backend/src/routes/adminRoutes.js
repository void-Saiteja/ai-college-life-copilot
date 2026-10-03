import express from 'express';
import { 
  getAdminDashboard,
  getStudents,
  getStudentById,
  getAcademicRisks,
  getAdminInsights,
  getAdminDocuments,
  getAdminStats, 
  createSubject, 
  createAnnouncement, 
  createFAQ 
} from '../controllers/adminController.js';
import { authenticateUser } from '../middleware/authMiddleware.js';
import { requireRole } from '../middleware/roleMiddleware.js';

const router = express.Router();

// Strict Admin Security: Must be authenticated and have ADMIN role
router.use(authenticateUser);
router.use(requireRole('ADMIN'));

// Admin Intelligence Endpoints
router.get('/dashboard', getAdminDashboard);
router.get('/students', getStudents);
router.get('/students/:studentId', getStudentById);
router.get('/risks', getAcademicRisks);
router.post('/insights', getAdminInsights);
router.get('/documents', getAdminDocuments);

// Preserved Admin Operations
router.get('/stats', getAdminStats);
router.post('/subjects', createSubject);
router.post('/announcements', createAnnouncement);
router.post('/faqs', createFAQ);

export default router;
