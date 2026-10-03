import { getDB, saveDB } from '../storage/db.js';
import { 
  calculateAdminDashboardStats, 
  getAdminStudentsList, 
  getAdminStudentDetail, 
  calculateAcademicRisks, 
  generateAdminInsightsAI, 
  getAdminDocumentsList 
} from '../services/adminIntelligenceService.js';
import { broadcastAnnouncementNotification } from '../services/notificationService.js';

/**
 * GET /api/admin/dashboard
 * Returns deterministic system overview statistics.
 */
export const getAdminDashboard = (req, res, next) => {
  try {
    const stats = calculateAdminDashboardStats();
    res.json({
      success: true,
      data: stats
    });
  } catch (error) {
    next(error);
  }
};

/**
 * GET /api/admin/students
 * Returns student list with academic metrics, risk indicators, and query filters.
 */
export const getStudents = (req, res, next) => {
  try {
    const { search, semester, department, attendanceRisk } = req.query;
    const students = getAdminStudentsList({ search, semester, department, attendanceRisk });
    res.json({
      success: true,
      data: students,
      count: students.length
    });
  } catch (error) {
    next(error);
  }
};

/**
 * GET /api/admin/students/:studentId
 * Returns detailed administrative drill-down for a selected student.
 */
export const getStudentById = (req, res, next) => {
  try {
    const { studentId } = req.params;
    if (!studentId || typeof studentId !== 'string') {
      return res.status(400).json({ success: false, error: 'Valid student ID is required' });
    }

    const detail = getAdminStudentDetail(studentId);
    if (!detail) {
      return res.status(404).json({ success: false, error: `Student with ID '${studentId}' not found` });
    }

    res.json({
      success: true,
      data: detail
    });
  } catch (error) {
    next(error);
  }
};

/**
 * GET /api/admin/risks
 * Returns categorized academic risks: attendance, assignment, exam, quiz, career.
 */
export const getAcademicRisks = (req, res, next) => {
  try {
    const risks = calculateAcademicRisks();
    res.json({
      success: true,
      data: risks
    });
  } catch (error) {
    next(error);
  }
};

/**
 * POST /api/admin/insights
 * Generates AI Executive Observations & Actionable Insights based on authoritative metrics.
 */
export const getAdminInsights = async (req, res, next) => {
  try {
    const dashboardStats = calculateAdminDashboardStats();
    const riskData = calculateAcademicRisks();

    const insights = await generateAdminInsightsAI({ dashboardStats, riskData });

    res.json({
      success: true,
      data: insights
    });
  } catch (error) {
    next(error);
  }
};

/**
 * GET /api/admin/documents
 * Returns documents with chunk count and embedding status.
 */
export const getAdminDocuments = (req, res, next) => {
  try {
    const docs = getAdminDocumentsList();
    res.json({
      success: true,
      data: docs
    });
  } catch (error) {
    next(error);
  }
};

// ==========================================
// PRESERVED EXISTING ADMIN OPERATIONS
// ==========================================

export const getAdminStats = (req, res, next) => {
  try {
    const db = getDB();

    res.json({
      success: true,
      data: {
        totalStudents: (db.students || []).length,
        totalDocuments: (db.documents || []).length,
        totalSubjects: (db.subjects || []).length,
        totalAssignments: (db.assignments || []).length,
        totalExams: (db.exams || []).length,
        totalChatSessions: (db.chat_sessions || []).length,
        recentAnnouncements: (db.announcements || []).slice(0, 5),
        faqs: db.faqs || []
      }
    });
  } catch (error) {
    next(error);
  }
};

export const createSubject = (req, res, next) => {
  try {
    const { code, name, instructor, credits = 3 } = req.body;
    if (!code || !name) {
      return res.status(400).json({ success: false, error: 'Course code and name are required' });
    }

    const db = getDB();
    const newSubject = {
      id: 'sub-' + Date.now(),
      code,
      name,
      instructor: instructor || 'Staff',
      credits: Number(credits)
    };

    db.subjects.push(newSubject);
    saveDB(db);

    res.status(201).json({ success: true, data: newSubject });
  } catch (error) {
    next(error);
  }
};

export const createAnnouncement = (req, res, next) => {
  try {
    const { title, content } = req.body;
    if (!title || !content) {
      return res.status(400).json({ success: false, error: 'Title and content are required' });
    }

    const db = getDB();
    const newAnc = {
      id: 'anc-' + Date.now(),
      title,
      content,
      posted_by: req.user?.id || 'u-admin-1',
      created_at: new Date().toISOString()
    };

    db.announcements.unshift(newAnc);
    saveDB(db);

    // Broadcast announcement as student notification
    broadcastAnnouncementNotification(newAnc);

    res.status(201).json({ success: true, data: newAnc });
  } catch (error) {
    next(error);
  }
};

export const createFAQ = (req, res, next) => {
  try {
    const { question, answer, category = 'Academic' } = req.body;
    if (!question || !answer) {
      return res.status(400).json({ success: false, error: 'Question and answer are required' });
    }

    const db = getDB();
    const newFaq = {
      id: 'faq-' + Date.now(),
      question,
      answer,
      category
    };

    db.faqs.push(newFaq);
    saveDB(db);

    res.status(201).json({ success: true, data: newFaq });
  } catch (error) {
    next(error);
  }
};
