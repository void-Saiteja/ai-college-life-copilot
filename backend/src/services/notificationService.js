import { getDB, saveDB } from '../storage/db.js';
import { calculateAttendanceMetrics } from './attendanceService.js';
import { calculateAssignmentMetrics } from './assignmentService.js';
import { calculateExamMetrics } from './examService.js';
import { calculateSkillGaps } from './careerService.js';
import { GoogleGenAI } from '@google/genai';
import { config } from '../config/env.js';

export const VALID_NOTIFICATION_TYPES = [
  'ATTENDANCE',
  'ASSIGNMENT',
  'EXAM',
  'QUIZ',
  'STUDY_PLAN',
  'CAREER',
  'ADMIN_ANNOUNCEMENT',
  'SYSTEM'
];

export const VALID_NOTIFICATION_PRIORITIES = [
  'LOW',
  'MEDIUM',
  'HIGH',
  'CRITICAL'
];

export const VALID_NOTIFICATION_SOURCES = [
  'ATTENDANCE',
  'ASSIGNMENT',
  'EXAM',
  'QUIZ',
  'STUDY_PLAN',
  'CAREER',
  'ADMIN',
  'SYSTEM'
];

/**
 * Validates notification type, priority, and source against defined enums.
 */
export function validateNotificationEnums({ type, priority, source }) {
  if (type && !VALID_NOTIFICATION_TYPES.includes(type)) return false;
  if (priority && !VALID_NOTIFICATION_PRIORITIES.includes(priority)) return false;
  if (source && !VALID_NOTIFICATION_SOURCES.includes(source)) return false;
  return true;
}

/**
 * Generates an optional AI-enhanced wording for notification while keeping facts locked.
 */
export async function enhanceNotificationWording({ title, message, type }) {
  const apiKey = process.env.GEMINI_API_KEY || config.geminiApiKey;
  if (!apiKey || apiKey === 'mock_key' || apiKey === 'your_gemini_api_key_here' || apiKey.trim() === '') {
    return { title, message, isAiEnhanced: false };
  }

  try {
    const ai = new GoogleGenAI({ apiKey });
    const promptText = `You are an AI notification assistant for college students.
Rephrase the following academic notification to be concise, polite, and encouraging.
CRITICAL CONSTRAINT: Do NOT change or invent any course names, percentages, dates, or deadlines.
ORIGINAL TITLE: "${title}"
ORIGINAL MESSAGE: "${message}"

Return JSON:
{
  "title": "Concise title",
  "message": "Polite actionable message"
}`;

    const response = await ai.models.generateContent({
      model: 'gemini-2.5-flash',
      contents: promptText
    });

    if (response && response.text) {
      const match = response.text.match(/\{[\s\S]*\}/);
      if (match) {
        const parsed = JSON.parse(match[0]);
        if (parsed.title && parsed.message) {
          return {
            title: parsed.title.trim(),
            message: parsed.message.trim(),
            isAiEnhanced: true
          };
        }
      }
    }
  } catch (err) {
    // Silent fallback to deterministic wording
  }

  return { title, message, isAiEnhanced: false };
}

/**
 * Generates automatic attendance notifications for a specific student.
 */
export function generateAttendanceNotifications(studentId, db) {
  const notifications = [];
  const attendance = (db.attendance || []).filter(a => a.student_id === studentId);
  const subjects = db.subjects || [];

  attendance.forEach(att => {
    const sub = subjects.find(s => s.id === att.subject_id);
    const subName = sub?.name || 'Subject';
    const subCode = sub?.code || 'CS';

    const metrics = calculateAttendanceMetrics({
      conducted: att.classes_conducted,
      attended: att.classes_attended,
      targetPercentage: 75
    });

    if (metrics.riskClassification === 'CRITICAL') {
      notifications.push({
        student_id: studentId,
        type: 'ATTENDANCE',
        priority: 'CRITICAL',
        title: `Attendance Critical Alert: ${subCode}`,
        message: `Your attendance in ${subName} (${subCode}) is ${metrics.percentage}%, below the required 75% target. You must attend the next ${metrics.minRequiredToAttend} consecutive classes.`,
        source: 'ATTENDANCE',
        source_id: `att-${att.id || att.subject_id}`
      });
    } else if (metrics.riskClassification === 'WARNING') {
      notifications.push({
        student_id: studentId,
        type: 'ATTENDANCE',
        priority: 'HIGH',
        title: `Attendance Warning: ${subCode}`,
        message: `Your attendance in ${subName} (${subCode}) is ${metrics.percentage}%. You are close to the 75% target. You can only miss ${metrics.maxCanMiss} more class(es).`,
        source: 'ATTENDANCE',
        source_id: `att-${att.id || att.subject_id}`
      });
    }
  });

  return notifications;
}

/**
 * Generates automatic assignment notifications for a specific student.
 */
export function generateAssignmentNotifications(studentId, db) {
  const notifications = [];
  const assignments = (db.assignments || []).filter(a => a.student_id === studentId || !a.student_id);

  assignments.forEach(asg => {
    if ((asg.status || '').toLowerCase() === 'completed') return;

    const m = calculateAssignmentMetrics(asg);

    if (m.deadlineStatus === 'OVERDUE') {
      notifications.push({
        student_id: studentId,
        type: 'ASSIGNMENT',
        priority: 'CRITICAL',
        title: `Overdue Assignment: ${asg.title}`,
        message: `Assignment "${asg.title}" is overdue (${Math.abs(m.daysRemaining)} day(s) past due). Please submit as soon as possible.`,
        source: 'ASSIGNMENT',
        source_id: asg.id
      });
    } else if (m.deadlineStatus === 'DUE_TODAY') {
      notifications.push({
        student_id: studentId,
        type: 'ASSIGNMENT',
        priority: 'CRITICAL',
        title: `Assignment Due Today: ${asg.title}`,
        message: `Assignment "${asg.title}" is due today. Ensure your submission is completed before the deadline.`,
        source: 'ASSIGNMENT',
        source_id: asg.id
      });
    } else if (m.deadlineStatus === 'DUE_SOON') {
      notifications.push({
        student_id: studentId,
        type: 'ASSIGNMENT',
        priority: 'HIGH',
        title: `Assignment Due Soon: ${asg.title}`,
        message: `Assignment "${asg.title}" is due in ${m.daysRemaining} days (${asg.dueDate || asg.due_date}).`,
        source: 'ASSIGNMENT',
        source_id: asg.id
      });
    }
  });

  return notifications;
}

/**
 * Generates automatic exam notifications for a specific student.
 */
export function generateExamNotifications(studentId, db) {
  const notifications = [];
  const exams = (db.exams || []).filter(e => e.student_id === studentId || !e.student_id);

  exams.forEach(exam => {
    if ((exam.status || '').toLowerCase() === 'completed') return;

    const m = calculateExamMetrics(exam);

    if (m.examStatus === 'TODAY') {
      notifications.push({
        student_id: studentId,
        type: 'EXAM',
        priority: 'CRITICAL',
        title: `Exam Today: ${exam.title}`,
        message: `Your ${exam.title} examination is scheduled for today at ${exam.start_time || '09:00 AM'}${exam.location ? ` in ${exam.location}` : ''}.`,
        source: 'EXAM',
        source_id: exam.id
      });
    } else if (m.examStatus === 'VERY_SOON') {
      notifications.push({
        student_id: studentId,
        type: 'EXAM',
        priority: 'HIGH',
        title: `Upcoming Exam in ${m.daysRemaining} Days: ${exam.title}`,
        message: `Your ${exam.title} exam is scheduled in ${m.daysRemaining} days (${exam.exam_date || exam.examDate}). Review your syllabus and study plan.`,
        source: 'EXAM',
        source_id: exam.id
      });
    } else if (m.examStatus === 'UPCOMING' && m.daysRemaining <= 7) {
      notifications.push({
        student_id: studentId,
        type: 'EXAM',
        priority: 'MEDIUM',
        title: `Upcoming Exam: ${exam.title}`,
        message: `Your ${exam.title} exam is scheduled in ${m.daysRemaining} days.`,
        source: 'EXAM',
        source_id: exam.id
      });
    }
  });

  return notifications;
}

/**
 * Generates automatic quiz notifications based on actual weak topics.
 */
export function generateQuizNotifications(studentId, db) {
  const notifications = [];
  const attempts = (db.quiz_attempts || []).filter(q => q.student_id === studentId);

  // Group by topic
  const topicScores = {};
  attempts.forEach(a => {
    const topicKey = a.topic || 'General Topic';
    if (!topicScores[topicKey]) topicScores[topicKey] = [];
    topicScores[topicKey].push(Number(a.percentage || 0));
  });

  Object.entries(topicScores).forEach(([topic, scores]) => {
    const avg = scores.reduce((sum, s) => sum + s, 0) / scores.length;
    if (avg < 60) {
      notifications.push({
        student_id: studentId,
        type: 'QUIZ',
        priority: 'MEDIUM',
        title: `Quiz Revision Alert: ${topic}`,
        message: `Your recent quiz performance indicates that ${topic} needs more practice (Average score: ${avg.toFixed(1)}%).`,
        source: 'QUIZ',
        source_id: `quiz-weak-${topic.toLowerCase().replace(/[^a-z0-9]/g, '-')}`
      });
    }
  });

  return notifications;
}

/**
 * Generates study plan notifications based on persisted scheduled study sessions.
 */
export function generateStudyPlanNotifications(studentId, db) {
  const notifications = [];
  const activePlan = (db.study_plans || []).find(p => p.student_id === studentId && (p.status || '').toLowerCase() === 'active');
  if (!activePlan) return notifications;

  const sessions = (db.study_sessions || []).filter(s => s.plan_id === activePlan.id && (s.status || '').toLowerCase() !== 'completed');
  const todayStr = new Date().toISOString().split('T')[0];

  sessions.forEach(sess => {
    if (sess.session_date === todayStr) {
      notifications.push({
        student_id: studentId,
        type: 'STUDY_PLAN',
        priority: 'MEDIUM',
        title: `Scheduled Study Session Today: ${sess.topic}`,
        message: `Your scheduled study session for "${sess.topic}" starts today${sess.start_time ? ` at ${sess.start_time}` : ''}. Priority: ${sess.priority || 'Medium'}.`,
        source: 'STUDY_PLAN',
        source_id: sess.id
      });
    }
  });

  return notifications;
}

/**
 * Generates career notifications based on authoritative deterministic skill gaps.
 */
export function generateCareerNotifications(studentId, db) {
  const notifications = [];
  const careerProfile = (db.career_profiles || []).find(p => p.student_id === studentId);
  if (!careerProfile) return notifications;

  const targetRole = careerProfile.target_role || 'Full Stack Developer';
  const gaps = calculateSkillGaps(careerProfile.skills || [], targetRole);

  if (gaps.missingList && gaps.missingList.length > 0) {
    const topMissing = gaps.missingList[0];
    notifications.push({
      student_id: studentId,
      type: 'CAREER',
      priority: 'MEDIUM',
      title: `Skill Gap Identified: ${topMissing}`,
      message: `You have a ${topMissing} skill gap for your ${targetRole} target role. Consider completing practice projects to bridge this gap.`,
      source: 'CAREER',
      source_id: `gap-${topMissing.toLowerCase()}`
    });
  }

  return notifications;
}

/**
 * Generates notifications for campus-wide admin announcements.
 */
export function generateAdminAnnouncementNotifications(studentId, db) {
  const notifications = [];
  const announcements = db.announcements || [];

  announcements.forEach(anc => {
    notifications.push({
      student_id: studentId,
      type: 'ADMIN_ANNOUNCEMENT',
      priority: 'HIGH',
      title: `Campus Announcement: ${anc.title}`,
      message: anc.content,
      source: 'ADMIN',
      source_id: anc.id
    });
  });

  return notifications;
}

/**
 * Master notification generator: Idempotently creates and saves notifications for a student.
 * Uses deterministic key (student_id + type + source + source_id) to prevent duplicate notifications.
 */
export function generateAllNotificationsForStudent(studentId) {
  const db = getDB();
  if (!Array.isArray(db.notifications)) db.notifications = [];

  const candidates = [
    ...generateAttendanceNotifications(studentId, db),
    ...generateAssignmentNotifications(studentId, db),
    ...generateExamNotifications(studentId, db),
    ...generateQuizNotifications(studentId, db),
    ...generateStudyPlanNotifications(studentId, db),
    ...generateCareerNotifications(studentId, db),
    ...generateAdminAnnouncementNotifications(studentId, db)
  ];

  // Set of existing deterministic keys
  const existingKeySet = new Set(
    db.notifications.map(n => `${n.student_id}::${n.type}::${n.source}::${n.source_id}`)
  );

  let createdCount = 0;
  const now = new Date().toISOString();

  candidates.forEach(c => {
    const key = `${c.student_id}::${c.type}::${c.source}::${c.source_id}`;
    if (!existingKeySet.has(key)) {
      const newNotif = {
        id: 'notif-' + Date.now() + '-' + Math.random().toString(36).substr(2, 6),
        student_id: c.student_id,
        type: c.type,
        priority: c.priority,
        title: c.title,
        message: c.message,
        source: c.source,
        source_id: c.source_id,
        is_read: false,
        created_at: now,
        expires_at: null
      };
      db.notifications.unshift(newNotif);
      existingKeySet.add(key);
      createdCount += 1;
    }
  });

  if (createdCount > 0) {
    saveDB(db);
  }

  const studentNotifs = db.notifications.filter(n => n.student_id === studentId);

  return {
    createdCount,
    totalCount: studentNotifs.length,
    unreadCount: studentNotifs.filter(n => !n.is_read).length
  };
}

/**
 * Broadcasts an admin announcement as notifications to all enrolled students.
 */
export function broadcastAnnouncementNotification(announcement) {
  const db = getDB();
  if (!Array.isArray(db.notifications)) db.notifications = [];
  const students = db.students || [];

  const now = new Date().toISOString();
  let count = 0;

  students.forEach(student => {
    const key = `${student.id}::ADMIN_ANNOUNCEMENT::ADMIN::${announcement.id}`;
    const exists = db.notifications.some(n => `${n.student_id}::${n.type}::${n.source}::${n.source_id}` === key);

    if (!exists) {
      db.notifications.unshift({
        id: 'notif-' + Date.now() + '-' + Math.random().toString(36).substr(2, 6),
        student_id: student.id,
        type: 'ADMIN_ANNOUNCEMENT',
        priority: 'HIGH',
        title: `Campus Announcement: ${announcement.title}`,
        message: announcement.content,
        source: 'ADMIN',
        source_id: announcement.id,
        is_read: false,
        created_at: now,
        expires_at: null
      });
      count += 1;
    }
  });

  if (count > 0) {
    saveDB(db);
  }

  return count;
}
