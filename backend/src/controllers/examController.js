import { getDB, saveDB } from '../storage/db.js';
import { calculateExamMetrics, generateExamPrioritization } from '../services/examService.js';
import { calculateAssignmentMetrics } from '../services/assignmentService.js';
import { calculateAttendanceMetrics } from '../services/attendanceService.js';
import { buildStudentContext } from '../services/contextService.js';

export const getExams = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { status = 'all' } = req.query;
    const db = getDB();
    const subjects = db.subjects || [];

    // Filter by studentId
    const userExams = (db.exams || []).filter(e => e.student_id === studentId);

    const formatted = userExams.map(e => {
      const subject = subjects.find(s => s.id === e.subject_id);
      const metrics = calculateExamMetrics(e);

      return {
        id: e.id,
        studentId: e.student_id || studentId,
        subjectId: e.subject_id || 'sub-1',
        subjectCode: subject?.code || 'CS',
        subjectName: subject?.name || 'Subject',
        title: e.title,
        exam_date: e.exam_date || e.examDate,
        start_time: e.start_time || e.startTime || '09:00 AM',
        location: e.location || 'Main Hall',
        syllabus: e.syllabus || 'Full syllabus',
        status: e.status || 'Upcoming',
        created_at: e.created_at || new Date().toISOString(),
        ...metrics
      };
    });

    // Apply status filter
    let filtered = formatted;
    const sLower = status.toLowerCase();
    if (sLower === 'upcoming') {
      filtered = formatted.filter(e => e.examStatus !== 'COMPLETED' && e.status !== 'Completed');
    } else if (sLower === 'completed') {
      filtered = formatted.filter(e => e.examStatus === 'COMPLETED' || e.status === 'Completed');
    } else if (sLower === 'today') {
      filtered = formatted.filter(e => e.examStatus === 'TODAY');
    } else if (sLower === 'very_soon' || sLower === 'very soon') {
      filtered = formatted.filter(e => e.examStatus === 'VERY_SOON' || e.examStatus === 'TODAY');
    }

    // Summary counts
    const summary = {
      total: formatted.length,
      upcoming: formatted.filter(e => e.examStatus !== 'COMPLETED' && e.status !== 'Completed').length,
      today: formatted.filter(e => e.examStatus === 'TODAY').length,
      verySoon: formatted.filter(e => e.examStatus === 'VERY_SOON').length,
      within7Days: formatted.filter(e => e.urgency === 'HIGH' || e.urgency === 'CRITICAL' || e.urgency === 'MEDIUM').length,
      completed: formatted.filter(e => e.examStatus === 'COMPLETED' || e.status === 'Completed').length
    };

    res.json({
      success: true,
      summary,
      data: filtered
    });
  } catch (error) {
    next(error);
  }
};

export const createExam = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { subject_id, title, exam_date, start_time, location, syllabus, status } = req.body;
    if (!title || !exam_date) {
      return res.status(400).json({ success: false, error: 'Title and exam date are required' });
    }

    const db = getDB();
    const subjects = db.subjects || [];
    const subject = subjects.find(s => s.id === subject_id) || subjects[0];

    const newExam = {
      id: 'exm-' + Date.now(),
      student_id: studentId,
      subject_id: subject?.id || 'sub-1',
      title,
      exam_date,
      start_time: start_time || '09:00 AM',
      location: location || 'Main Hall',
      syllabus: syllabus || 'Full syllabus',
      status: status || 'Upcoming',
      created_at: new Date().toISOString()
    };

    db.exams.push(newExam);
    saveDB(db);

    const metrics = calculateExamMetrics(newExam);

    res.status(201).json({
      success: true,
      data: {
        ...newExam,
        subjectCode: subject?.code || 'CS',
        subjectName: subject?.name || 'Subject',
        ...metrics
      }
    });
  } catch (error) {
    next(error);
  }
};

export const updateExam = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();
    const index = db.exams.findIndex(e => e.id === id);

    if (index === -1) {
      return res.status(404).json({ success: false, error: 'Exam not found' });
    }

    const existing = db.exams[index];

    // Ownership check: Student can only update their own exam
    const isOwner = existing.student_id === studentId || (!existing.student_id && studentId === 'std-1');
    if (!isOwner) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this exam record' });
    }

    const updated = {
      ...existing,
      ...req.body,
      student_id: existing.student_id || studentId
    };

    db.exams[index] = updated;
    saveDB(db);

    const subjects = db.subjects || [];
    const subject = subjects.find(s => s.id === updated.subject_id);
    const metrics = calculateExamMetrics(updated);

    res.json({
      success: true,
      data: {
        ...updated,
        subjectCode: subject?.code || 'CS',
        subjectName: subject?.name || 'Subject',
        ...metrics
      }
    });
  } catch (error) {
    next(error);
  }
};

export const deleteExam = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();
    const existing = db.exams.find(e => e.id === id);

    if (!existing) {
      return res.status(404).json({ success: false, error: 'Exam not found' });
    }

    // Ownership check: Student can only delete their own exam
    const isOwner = existing.student_id === studentId || (!existing.student_id && studentId === 'std-1');
    if (!isOwner) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this exam record' });
    }

    db.exams = db.exams.filter(e => e.id !== id);
    saveDB(db);

    res.json({ success: true, message: 'Exam deleted successfully' });
  } catch (error) {
    next(error);
  }
};

export const runExamAdvisor = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();
    const subjects = db.subjects || [];

    // Filter student exams
    const userExams = (db.exams || []).filter(e => e.student_id === studentId);
    const formattedExams = userExams.map(e => {
      const sub = subjects.find(s => s.id === e.subject_id);
      return {
        ...e,
        subjectCode: sub?.code || 'CS',
        subjectName: sub?.name || 'Subject',
        ...calculateExamMetrics(e)
      };
    });

    // Filter student assignments
    const userAssignments = (db.assignments || []).filter(a => a.student_id === studentId || !a.student_id);
    const formattedAssignments = userAssignments.map(a => {
      const sub = subjects.find(s => s.id === a.subject_id || s.code === a.course || s.name === a.course);
      return {
        ...a,
        course: a.course || sub?.code || 'CS',
        subjectName: sub?.name || 'Subject',
        ...calculateAssignmentMetrics(a)
      };
    });

    // Filter student attendance
    const userAttendance = (db.attendance || []).filter(a => a.student_id === studentId).map(a => {
      const sub = subjects.find(s => s.id === a.subject_id);
      return {
        subject: sub?.name || 'Subject',
        code: sub?.code || 'CS',
        ...calculateAttendanceMetrics({ conducted: a.classes_conducted, attended: a.classes_attended })
      };
    });

    const studentContext = buildStudentContext(studentId);

    const aiResult = await generateExamPrioritization({
      exams: formattedExams,
      assignments: formattedAssignments,
      attendance: userAttendance,
      studentContext
    });

    res.json({
      success: true,
      data: aiResult.data
    });
  } catch (error) {
    next(error);
  }
};
