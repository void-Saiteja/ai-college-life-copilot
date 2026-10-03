import express from 'express';
import { 
  getCareerProfile, 
  updateCareerProfile, 
  addSkill, 
  updateSkill, 
  deleteSkill, 
  runCareerAdvisor, 
  runLearningRoadmap,
  analyzeResume, 
  practiceInterview,
  getResumes,
  createResume,
  getResumeById,
  updateResume,
  deleteResume,
  analyzeResumeById,
  generateInterviewQuestion,
  submitInterviewAnswer,
  getInterviewHistory
} from '../controllers/careerController.js';
import { authenticateUser } from '../middleware/authMiddleware.js';

const router = express.Router();

// Career Profile
router.get('/profile', authenticateUser, getCareerProfile);
router.post('/profile', authenticateUser, updateCareerProfile);
router.put('/profile', authenticateUser, updateCareerProfile);

// Skills
router.post('/skills', authenticateUser, addSkill);
router.put('/skills/:skillName', authenticateUser, updateSkill);
router.delete('/skills/:skillName', authenticateUser, deleteSkill);

// Advisory & Roadmap
router.post('/advisor', authenticateUser, runCareerAdvisor);
router.post('/roadmap', authenticateUser, runLearningRoadmap);

// Backward Compatibility Endpoints
router.post('/analyze-resume', authenticateUser, analyzeResume);
router.post('/interview-practice', authenticateUser, practiceInterview);

// Resume Intelligence Endpoints
router.get('/resumes', authenticateUser, getResumes);
router.post('/resumes', authenticateUser, createResume);
router.get('/resumes/:id', authenticateUser, getResumeById);
router.put('/resumes/:id', authenticateUser, updateResume);
router.delete('/resumes/:id', authenticateUser, deleteResume);
router.post('/resumes/:id/analyze', authenticateUser, analyzeResumeById);

// Interview Intelligence Endpoints
router.post('/interview/generate', authenticateUser, generateInterviewQuestion);
router.post('/interview/:id/answer', authenticateUser, submitInterviewAnswer);
router.get('/interview/history', authenticateUser, getInterviewHistory);

export default router;
