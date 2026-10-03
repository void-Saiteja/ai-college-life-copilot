import { buildStudentContext } from '../services/contextService.js';
import { getDB } from '../storage/db.js';

export const getDashboardStats = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }
    const context = buildStudentContext(studentId);
    const db = getDB();

    const notifications = (db.notifications || []).filter(n => n.student_id === studentId);

    res.json({
      success: true,
      data: {
        ...context,
        notifications
      }
    });
  } catch (error) {
    next(error);
  }
};
