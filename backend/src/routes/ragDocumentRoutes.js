import express from 'express';
import multer from 'multer';
import { getDocuments, uploadDocument, queryRAG, deleteDocument } from '../controllers/ragDocumentController.js';
import { authenticateUser } from '../middleware/authMiddleware.js';
import { requireRole } from '../middleware/roleMiddleware.js';

const upload = multer({ limits: { fileSize: 10 * 1024 * 1024 } });
const router = express.Router();

router.get('/', authenticateUser, getDocuments);
router.post('/upload', authenticateUser, requireRole('ADMIN'), upload.single('file'), uploadDocument);
router.post('/query', authenticateUser, queryRAG);
router.delete('/:id', authenticateUser, requireRole('ADMIN'), deleteDocument);

export default router;
