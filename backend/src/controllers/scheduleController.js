import { getDB, saveDB } from '../storage/db.js';

const VALID_DAYS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

export const getSchedule = (req, res, next) => {
  try {
    const studentKey = req.user?.studentId || req.user?.id;
    if (!studentKey) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();
    // Only return schedule items owned by this student
    const studentSchedule = (db.schedule || []).filter(
      s => s.student_id === studentKey || (!s.student_id && studentKey === 'std-1')
    );

    res.json({ success: true, data: studentSchedule });
  } catch (error) {
    next(error);
  }
};

export const addScheduleItem = (req, res, next) => {
  try {
    const studentKey = req.user?.studentId || req.user?.id;
    if (!studentKey) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { course, room, days, time, instructor } = req.body;

    // Strict Input Validation
    if (!course || typeof course !== 'string' || course.trim().length === 0 || course.length > 100) {
      return res.status(400).json({ success: false, error: 'Valid course name is required (1-100 characters)' });
    }

    if (!time || typeof time !== 'string' || time.trim().length === 0 || time.length > 50) {
      return res.status(400).json({ success: false, error: 'Valid time slot is required (1-50 characters)' });
    }

    if (room && (typeof room !== 'string' || room.length > 50)) {
      return res.status(400).json({ success: false, error: 'Room location must be a string up to 50 characters' });
    }

    if (instructor && (typeof instructor !== 'string' || instructor.length > 100)) {
      return res.status(400).json({ success: false, error: 'Instructor must be a string up to 100 characters' });
    }

    const rawDays = Array.isArray(days) ? days : (typeof days === 'string' ? [days] : []);
    if (rawDays.length === 0) {
      return res.status(400).json({ success: false, error: 'At least one valid active day is required' });
    }

    const invalidDay = rawDays.find(d => !VALID_DAYS.includes(d));
    if (invalidDay) {
      return res.status(400).json({
        success: false,
        error: `Invalid day '${invalidDay}'. Allowed days: ${VALID_DAYS.join(', ')}`
      });
    }

    const db = getDB();
    const newItem = {
      id: 'sch-' + Date.now(),
      student_id: studentKey,
      course: course.trim(),
      room: room ? room.trim() : 'TBD',
      days: rawDays,
      time: time.trim(),
      instructor: instructor ? instructor.trim() : 'Staff',
      created_at: new Date().toISOString()
    };

    db.schedule.push(newItem);
    saveDB(db);

    res.status(201).json({ success: true, data: newItem });
  } catch (error) {
    next(error);
  }
};

export const deleteScheduleItem = (req, res, next) => {
  try {
    const studentKey = req.user?.studentId || req.user?.id;
    if (!studentKey) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();
    const item = (db.schedule || []).find(s => s.id === id);

    if (!item) {
      return res.status(404).json({ success: false, error: 'Class schedule item not found' });
    }

    // Ownership check: Student can only delete their own schedule item
    const isOwner = item.student_id === studentKey || (!item.student_id && studentKey === 'std-1');
    if (!isOwner) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this schedule item' });
    }

    db.schedule = db.schedule.filter(s => s.id !== id);
    saveDB(db);
    res.json({ success: true, message: 'Class schedule item removed' });
  } catch (error) {
    next(error);
  }
};
