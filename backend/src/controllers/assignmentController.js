import { getDB, saveDB } from '../storage/db.js';
import { calculateAssignmentMetrics, generateAssignmentPrioritization } from '../services/assignmentService.js';
import { buildStudentContext } from '../services/contextService.js';

export const getAssignments = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { status = 'all' } = req.query;
    const db = getDB();

    const subjects = db.subjects || [];

    // Filter strictly by studentId (unassigned legacy seed data belongs only to default student std-1)
    const userAssignments = (db.assignments || []).filter(a => a.student_id === studentId || (!a.student_id && studentId === 'std-1'));

    const formatted = userAssignments.map(a => {
      const sub = subjects.find(s => s.id === a.subject_id || s.code === a.course || s.name === a.course);
      const metrics = calculateAssignmentMetrics(a);

      return {
        id: a.id,
        studentId: a.student_id || studentId,
        subjectId: a.subject_id || sub?.id || 'sub-1',
        course: a.course || sub?.code || sub?.name || 'Course',
        subjectName: sub?.name || a.course || 'Course',
        title: a.title,
        description: a.description || '',
        dueDate: a.dueDate || a.due_date,
        status: a.status || 'Pending',
        priority: a.priority || metrics.urgency,
        estimatedHours: Number(a.estimatedHours || a.estimated_hours || 2),
        category: a.category || 'Assignment',
        created_at: a.created_at || new Date().toISOString(),
        completed_at: a.completed_at || null,
        ...metrics
      };
    });

    // Apply filtering if specified
    let filtered = formatted;
    const sLower = status.toLowerCase();
    if (sLower === 'pending') {
      filtered = formatted.filter(a => a.deadlineStatus !== 'COMPLETED' && a.status !== 'Completed');
    } else if (sLower === 'completed') {
      filtered = formatted.filter(a => a.deadlineStatus === 'COMPLETED' || a.status === 'Completed');
    } else if (sLower === 'overdue') {
      filtered = formatted.filter(a => a.deadlineStatus === 'OVERDUE');
    } else if (sLower === 'due_soon') {
      filtered = formatted.filter(a => a.deadlineStatus === 'DUE_SOON' || a.deadlineStatus === 'DUE_TODAY');
    }

    // Summary counts
    const summary = {
      total: formatted.length,
      pending: formatted.filter(a => a.deadlineStatus !== 'COMPLETED' && a.status !== 'Completed').length,
      completed: formatted.filter(a => a.deadlineStatus === 'COMPLETED' || a.status === 'Completed').length,
      overdue: formatted.filter(a => a.deadlineStatus === 'OVERDUE').length,
      dueSoon: formatted.filter(a => a.deadlineStatus === 'DUE_SOON' || a.deadlineStatus === 'DUE_TODAY').length
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

export const createAssignment = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { title, course, subject_id, dueDate, priority, estimatedHours, category, description } = req.body;
    if (!title) {
      return res.status(400).json({ success: false, error: 'Title is required' });
    }

    const db = getDB();
    const subjects = db.subjects || [];
    const sub = subjects.find(s => s.id === subject_id || s.code === course || s.name === course);

    const newAssignment = {
      id: 'asg-' + Date.now(),
      student_id: studentId,
      subject_id: sub?.id || subject_id || 'sub-1',
      title,
      course: course || sub?.code || 'CS',
      description: description || '',
      dueDate: dueDate || new Date().toISOString().split('T')[0],
      priority: priority || 'Medium',
      status: 'Pending',
      estimatedHours: Number(estimatedHours) || 2,
      category: category || 'Assignment',
      created_at: new Date().toISOString()
    };

    db.assignments.unshift(newAssignment);
    saveDB(db);

    const metrics = calculateAssignmentMetrics(newAssignment);

    res.status(201).json({
      success: true,
      data: {
        ...newAssignment,
        ...metrics
      }
    });
  } catch (error) {
    next(error);
  }
};

export const updateAssignment = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();
    const index = db.assignments.findIndex(a => a.id === id);

    if (index === -1) {
      return res.status(404).json({ success: false, error: 'Assignment not found' });
    }

    const existing = db.assignments[index];

    // Authorization check: Ensure assignment belongs to authenticated student
    const isOwner = existing.student_id === studentId || (!existing.student_id && studentId === 'std-1');
    if (!isOwner) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this assignment' });
    }

    const updated = {
      ...existing,
      ...req.body,
      student_id: existing.student_id || studentId
    };

    if (req.body.status === 'Completed' && existing.status !== 'Completed') {
      updated.completed_at = new Date().toISOString();
    } else if (req.body.status && req.body.status !== 'Completed') {
      updated.completed_at = null;
    }

    db.assignments[index] = updated;
    saveDB(db);

    const metrics = calculateAssignmentMetrics(updated);

    res.json({
      success: true,
      data: {
        ...updated,
        ...metrics
      }
    });
  } catch (error) {
    next(error);
  }
};

export const deleteAssignment = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();
    const existing = db.assignments.find(a => a.id === id);

    if (!existing) {
      return res.status(404).json({ success: false, error: 'Assignment not found' });
    }

    // Authorization check
    const isOwner = existing.student_id === studentId || (!existing.student_id && studentId === 'std-1');
    if (!isOwner) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this assignment' });
    }

    db.assignments = db.assignments.filter(a => a.id !== id);
    saveDB(db);

    res.json({ success: true, message: 'Assignment deleted successfully' });
  } catch (error) {
    next(error);
  }
};

export const runAssignmentAdvisor = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();
    const userAssignments = (db.assignments || []).filter(a => a.student_id === studentId || (!a.student_id && studentId === 'std-1'));

    const formattedAssignments = userAssignments.map(a => ({
      ...a,
      ...calculateAssignmentMetrics(a)
    }));

    const studentContext = buildStudentContext(studentId);

    const aiResult = await generateAssignmentPrioritization({
      assignments: formattedAssignments,
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
