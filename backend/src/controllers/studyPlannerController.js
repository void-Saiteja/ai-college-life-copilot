import { getDB, saveDB } from '../storage/db.js';
import { buildStudentContext } from '../services/contextService.js';
import { generateStructuredStudyPlan } from '../services/llmService.js';

export const getStudyPlan = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    const db = getDB();

    if (!studentId) {
      return res.json({
        success: true,
        data: {
          plan: null,
          sessions: [],
          structuredPlan: null
        }
      });
    }

    const plan = (db.study_plans || []).find(p => p.student_id === studentId && p.status === 'Active') || null;
    const sessions = plan ? (db.study_sessions || []).filter(s => s.plan_id === plan.id) : [];

    const subjects = db.subjects || [];
    const formattedSessions = sessions.map(s => {
      const sub = subjects.find(sub => sub.id === s.subject_id || sub.name === s.subjectName);
      return {
        ...s,
        subjectCode: sub?.code || 'CS',
        subjectName: s.subjectName || sub?.name || 'Subject'
      };
    });

    res.json({
      success: true,
      data: {
        plan,
        sessions: formattedSessions,
        structuredPlan: plan?.ai_metadata || null
      }
    });
  } catch (error) {
    next(error);
  }
};

export const generateStudyPlan = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(403).json({ success: false, error: 'Access denied: Only students can generate study plans' });
    }

    const { 
      title = 'Midterm Exam Sprint 2026', 
      availableHoursPerDay = 3, 
      startTime = '16:00', 
      endTime = '21:00',
      studyDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
      focusSubjects = '' 
    } = req.body;

    const db = getDB();

    // 1. Gather real student academic context
    const context = buildStudentContext(studentId);

    // 2. Generate structured AI Study Plan using Gemini LLM
    const aiResult = await generateStructuredStudyPlan({
      context,
      preferences: {
        title,
        availableHoursPerDay: Number(availableHoursPerDay),
        startTime,
        endTime,
        studyDays,
        focusSubjects
      }
    });

    const structuredPlan = aiResult.data;

    const planId = 'sp-' + Date.now();
    const startDate = new Date().toISOString().split('T')[0];
    const endDate = new Date(Date.now() + 86400000 * 7).toISOString().split('T')[0];

    const newPlan = {
      id: planId,
      student_id: studentId,
      title,
      start_date: startDate,
      end_date: endDate,
      status: 'Active',
      ai_metadata: structuredPlan
    };

    // Mark previous student plans as completed
    db.study_plans.forEach(p => { if (p.student_id === studentId) p.status = 'Completed'; });
    db.study_plans.unshift(newPlan);

    // Convert structured daily tasks into database study sessions
    const generatedSessions = [];
    const subjects = db.subjects || [];

    if (structuredPlan && Array.isArray(structuredPlan.daily_plan)) {
      let idxCount = 1;
      structuredPlan.daily_plan.forEach(dayObj => {
        if (dayObj.tasks && Array.isArray(dayObj.tasks)) {
          dayObj.tasks.forEach(task => {
            const subMatch = subjects.find(s => s.name.toLowerCase().includes((task.subject || '').toLowerCase()) || (task.subject || '').toLowerCase().includes(s.name.toLowerCase()));
            
            generatedSessions.push({
              id: `ss-${Date.now()}-${idxCount}`,
              plan_id: planId,
              subject_id: subMatch?.id || 'sub-1',
              subjectCode: subMatch?.code || 'CS',
              subjectName: task.subject || subMatch?.name || 'General Study',
              topic: task.activity || 'Subject Study & Problem Solving',
              reason: task.reason || 'Curriculum Focus',
              session_date: dayObj.date || startDate,
              start_time: task.start_time || startTime,
              end_time: task.end_time || '18:00',
              priority: task.priority || 'High',
              status: 'Pending'
            });
            idxCount++;
          });
        }
      });
    }

    db.study_sessions.push(...generatedSessions);
    saveDB(db);

    res.status(201).json({
      success: true,
      message: 'AI Personalized Study Plan generated successfully',
      data: {
        plan: newPlan,
        sessions: generatedSessions,
        structuredPlan
      }
    });

  } catch (error) {
    next(error);
  }
};

export const updateSessionStatus = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    const { id } = req.params;
    const { status } = req.body;
    const db = getDB();

    if (!studentId) {
      return res.status(403).json({ success: false, error: 'Access denied: Only students can update session statuses' });
    }

    const session = (db.study_sessions || []).find(s => s.id === id);
    if (!session) {
      return res.status(404).json({ success: false, error: 'Session not found' });
    }

    // Ownership check: verify session's plan belongs to authenticated student
    const plan = (db.study_plans || []).find(p => p.id === session.plan_id && p.student_id === studentId);
    if (!plan) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this study session' });
    }

    const validStatuses = ['Pending', 'In Progress', 'Completed', 'Skipped'];
    if (!status || !validStatuses.includes(status)) {
      return res.status(400).json({
        success: false,
        error: `Invalid status '${status}'. Allowed statuses: ${validStatuses.join(', ')}`
      });
    }

    session.status = status;
    saveDB(db);

    res.json({ success: true, data: session });
  } catch (error) {
    next(error);
  }
};

export const replanSchedule = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    const db = getDB();

    if (!studentId) {
      return res.status(403).json({ success: false, error: 'Access denied: Only students can replan study schedules' });
    }

    const plan = db.study_plans.find(p => p.student_id === studentId && p.status === 'Active');
    if (!plan) {
      return res.status(404).json({ success: false, error: 'No active study plan to replan' });
    }

    // Shift all non-completed sessions forward
    let dayOffset = 1;
    db.study_sessions.forEach(s => {
      if (s.plan_id === plan.id && s.status !== 'Completed') {
        s.session_date = new Date(Date.now() + 86400000 * dayOffset).toISOString().split('T')[0];
        s.status = 'Pending';
        dayOffset += 1;
      }
    });

    saveDB(db);

    res.json({
      success: true,
      message: 'Replanned remaining sessions successfully! Completed sessions preserved.',
      data: db.study_sessions.filter(s => s.plan_id === plan.id)
    });
  } catch (error) {
    next(error);
  }
};
