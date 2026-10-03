import { getDB, saveDB } from '../storage/db.js';
import { 
  generateAllNotificationsForStudent, 
  VALID_NOTIFICATION_TYPES,
  VALID_NOTIFICATION_PRIORITIES,
  VALID_NOTIFICATION_SOURCES,
  validateNotificationEnums
} from '../services/notificationService.js';

/**
 * GET /api/notifications
 * Returns notifications strictly belonging to the authenticated student.
 * Supports filters: ?status=all|unread|read, ?type=...
 */
export const getNotifications = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { status, type } = req.query;
    const db = getDB();
    const allNotifs = db.notifications || [];

    // Filter strictly by authenticated student_id
    let studentNotifs = allNotifs.filter(n => n.student_id === studentId);

    // Apply status filter
    if (status === 'unread') {
      studentNotifs = studentNotifs.filter(n => !n.is_read);
    } else if (status === 'read') {
      studentNotifs = studentNotifs.filter(n => n.is_read);
    }

    // Apply type filter
    if (type && VALID_NOTIFICATION_TYPES.includes(type.toUpperCase())) {
      studentNotifs = studentNotifs.filter(n => n.type === type.toUpperCase());
    }

    // Filter out expired notifications if expires_at is set in the past
    const now = new Date();
    studentNotifs = studentNotifs.filter(n => {
      if (!n.expires_at) return true;
      return new Date(n.expires_at) > now;
    });

    // Sort newest first
    studentNotifs.sort((a, b) => new Date(b.created_at).getTime() - new Date(a.created_at).getTime());

    const unreadCount = (db.notifications || []).filter(n => n.student_id === studentId && !n.is_read).length;

    res.json({
      success: true,
      data: {
        notifications: studentNotifs,
        unreadCount,
        count: studentNotifs.length
      }
    });
  } catch (error) {
    next(error);
  }
};

/**
 * GET /api/notifications/unread-count
 * Returns unread notification count for the authenticated student.
 */
export const getUnreadCount = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();
    const count = (db.notifications || []).filter(n => n.student_id === studentId && !n.is_read).length;

    res.json({
      success: true,
      count
    });
  } catch (error) {
    next(error);
  }
};

/**
 * PUT /api/notifications/:id/read
 * Marks a single notification as read with strict ownership check.
 */
export const markAsRead = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();

    const notif = (db.notifications || []).find(n => n.id === id);
    if (!notif) {
      return res.status(404).json({ success: false, error: 'Notification not found' });
    }

    // Ownership Verification
    if (notif.student_id !== studentId) {
      return res.status(403).json({ success: false, error: 'Forbidden: Access denied to notification' });
    }

    notif.is_read = true;
    saveDB(db);

    res.json({
      success: true,
      message: 'Notification marked as read',
      data: notif
    });
  } catch (error) {
    next(error);
  }
};

/**
 * PUT /api/notifications/read-all
 * Marks all notifications for the authenticated student as read.
 */
export const markAllAsRead = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();
    let updatedCount = 0;

    (db.notifications || []).forEach(n => {
      if (n.student_id === studentId && !n.is_read) {
        n.is_read = true;
        updatedCount += 1;
      }
    });

    if (updatedCount > 0) {
      saveDB(db);
    }

    res.json({
      success: true,
      message: 'All notifications marked as read',
      updatedCount
    });
  } catch (error) {
    next(error);
  }
};

/**
 * DELETE /api/notifications/:id
 * Allows a student to delete only their own notification.
 */
export const deleteNotification = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();

    const notif = (db.notifications || []).find(n => n.id === id);
    if (!notif) {
      return res.status(404).json({ success: false, error: 'Notification not found' });
    }

    // Ownership Verification
    if (notif.student_id !== studentId) {
      return res.status(403).json({ success: false, error: 'Forbidden: Access denied to notification' });
    }

    db.notifications = (db.notifications || []).filter(n => n.id !== id);
    saveDB(db);

    res.json({
      success: true,
      message: 'Notification deleted successfully'
    });
  } catch (error) {
    next(error);
  }
};

/**
 * POST /api/notifications/generate
 * Generates applicable notifications from authoritative services without duplicates.
 */
export const generateNotifications = (req, res, next) => {
  try {
    const targetStudentId = req.body?.student_id;
    const currentStudentId = req.user?.studentId;
    const isAdmin = req.user?.role === 'ADMIN';

    // Student A cannot generate notifications for Student B
    if (!isAdmin && targetStudentId && targetStudentId !== currentStudentId) {
      return res.status(403).json({
        success: false,
        error: 'Forbidden: Students cannot generate notifications for other students'
      });
    }

    if (!isAdmin && !currentStudentId) {
      return res.status(401).json({
        success: false,
        error: 'Unauthorized: Student authentication required'
      });
    }

    const db = getDB();

    // If Admin requests generation for all students
    if (isAdmin && !targetStudentId) {
      let totalCreated = 0;
      (db.students || []).forEach(s => {
        const result = generateAllNotificationsForStudent(s.id);
        totalCreated += result.createdCount;
      });
      return res.json({
        success: true,
        message: 'Generated notifications for all students',
        createdCount: totalCreated
      });
    }

    const studentToGenerate = isAdmin ? targetStudentId : currentStudentId;
    const result = generateAllNotificationsForStudent(studentToGenerate);

    res.json({
      success: true,
      data: result
    });
  } catch (error) {
    next(error);
  }
};
