import express from 'express';
import cors from 'cors';
import { config } from './src/config/env.js';
import { initDB } from './src/config/db.js';

import healthRoutes from './src/routes/healthRoutes.js';
import authRoutes from './src/routes/authRoutes.js';
import studentRoutes from './src/routes/studentRoutes.js';
import attendanceRoutes from './src/routes/attendanceRoutes.js';
import assignmentRoutes from './src/routes/assignmentRoutes.js';
import examRoutes from './src/routes/examRoutes.js';
import studyPlannerRoutes from './src/routes/studyPlannerRoutes.js';
import aiCopilotRoutes from './src/routes/aiCopilotRoutes.js';
import ragDocumentRoutes from './src/routes/ragDocumentRoutes.js';
import quizRoutes from './src/routes/quizRoutes.js';
import careerRoutes from './src/routes/careerRoutes.js';
import adminRoutes from './src/routes/adminRoutes.js';
import notificationRoutes from './src/routes/notificationRoutes.js';
import aiRoutes from './src/routes/aiRoutes.js';
import budgetRoutes from './src/routes/budgetRoutes.js';
import scheduleRoutes from './src/routes/scheduleRoutes.js';
import notesRoutes from './src/routes/notesRoutes.js';

import { notFoundHandler } from './src/middleware/notFoundHandler.js';
import { errorHandler } from './src/middleware/errorHandler.js';
import { securityHeaders } from './src/middleware/securityHeaders.js';
import { createRateLimiter } from './src/middleware/rateLimiter.js';

const app = express();

// Disable Express fingerprint
app.disable('x-powered-by');

// 1. Security Headers (nosniff, clickjacking, xss, referrer)
app.use(securityHeaders);

// 2. Safe CORS Configuration
const allowedOrigins = config.clientOrigin && config.clientOrigin !== '*'
  ? config.clientOrigin.split(',').map(o => o.trim())
  : ['http://localhost:5173', 'http://127.0.0.1:5173'];

app.use(cors({
  origin: (origin, callback) => {
    if (!origin) return callback(null, true);
    if (config.nodeEnv !== 'production' || allowedOrigins.includes(origin) || config.clientOrigin === '*') {
      return callback(null, true);
    }
    return callback(new Error('Blocked by CORS policy'));
  },
  credentials: true
}));

// 3. Request Body Size Limits (Protection against memory exhaustion)
app.use(express.json({ limit: '2mb' }));
app.use(express.urlencoded({ extended: true, limit: '2mb' }));

// 4. Rate Limiting Middleware
const generalLimiter = createRateLimiter({
  windowMs: config.rateLimit.windowMs,
  maxRequests: config.rateLimit.generalMax,
  message: 'Too many API requests. Please try again later.'
});

const authLimiter = createRateLimiter({
  windowMs: config.rateLimit.windowMs,
  maxRequests: config.rateLimit.authMax,
  message: 'Too many authentication attempts. Please try again after 1 minute.'
});

app.use('/api', generalLimiter);
app.use('/api/auth/login', authLimiter);
app.use('/api/auth/register', authLimiter);

// Request logger
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  next();
});

// API Routes
app.use('/api', healthRoutes);
app.use('/api/auth', authRoutes);
app.use('/api/student', studentRoutes);
app.use('/api/attendance', attendanceRoutes);
app.use('/api/assignments', assignmentRoutes);
app.use('/api/exams', examRoutes);
app.use('/api/planner', studyPlannerRoutes);
app.use('/api/chat', aiCopilotRoutes);
app.use('/api/documents', ragDocumentRoutes);
app.use('/api/quiz', quizRoutes);
app.use('/api/quizzes', quizRoutes);
app.use('/api/career', careerRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/notifications', notificationRoutes);
app.use('/api/ai', aiRoutes);
app.use('/api/budget', budgetRoutes);
app.use('/api/schedule', scheduleRoutes);
app.use('/api/notes', notesRoutes);

// Error Handling
app.use(notFoundHandler);
app.use(errorHandler);

// Initialize DB & Listen
initDB().then(() => {
  app.listen(config.port, () => {
    console.log(`==================================================`);
    console.log(`🚀 AI College Life Copilot Backend Active`);
    console.log(`📡 URL: http://localhost:${config.port}`);
    console.log(`🏥 Health Check: http://localhost:${config.port}/api/health`);
    console.log(`==================================================`);
  });
});
