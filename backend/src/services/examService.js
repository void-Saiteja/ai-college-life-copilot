import { GoogleGenAI } from '@google/genai';
import { config } from '../config/env.js';

/**
 * Deterministically calculates days remaining, exam status, and urgency.
 */
export function calculateExamMetrics(exam) {
  const statusRaw = (exam.status || 'Upcoming').trim();
  const isCompleted = statusRaw.toLowerCase() === 'completed';

  if (isCompleted) {
    return {
      daysRemaining: 0,
      examStatus: 'COMPLETED',
      urgency: 'COMPLETED'
    };
  }

  const examDateStr = exam.exam_date || exam.examDate;
  if (!examDateStr) {
    return {
      daysRemaining: 999,
      examStatus: 'UPCOMING',
      urgency: 'LOW'
    };
  }

  // Parse YYYY-MM-DD in local time
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const [year, month, day] = examDateStr.split('-').map(Number);
  const examDate = new Date(year, month - 1, day);
  examDate.setHours(0, 0, 0, 0);

  const diffTime = examDate.getTime() - today.getTime();
  const daysRemaining = Math.round(diffTime / (1000 * 60 * 60 * 24));

  // If exam date has passed and not marked completed, treat as completed
  if (daysRemaining < 0) {
    return {
      daysRemaining,
      examStatus: 'COMPLETED',
      urgency: 'COMPLETED'
    };
  }

  // Exam Status Logic
  let examStatus = 'UPCOMING';
  if (daysRemaining === 0) {
    examStatus = 'TODAY';
  } else if (daysRemaining <= 3) {
    examStatus = 'VERY_SOON';
  } else {
    examStatus = 'UPCOMING';
  }

  // Urgency / Priority Logic
  let urgency = 'LOW';
  if (daysRemaining === 0) {
    urgency = 'CRITICAL';
  } else if (daysRemaining <= 3) {
    urgency = 'HIGH';
  } else if (daysRemaining <= 7) {
    urgency = 'MEDIUM';
  } else {
    urgency = 'LOW';
  }

  return {
    daysRemaining,
    examStatus,
    urgency
  };
}

/**
 * Generates AI Exam Advisor recommendations grounded strictly in authoritative backend data.
 */
export async function generateExamPrioritization({ exams = [], assignments = [], attendance = [], studentContext = {} }) {
  const apiKey = process.env.GEMINI_API_KEY || config.geminiApiKey;

  const upcomingExams = exams.filter(e => e.examStatus !== 'COMPLETED' && e.status !== 'Completed');

  if (upcomingExams.length === 0) {
    return {
      success: true,
      data: {
        summary: '🎉 No upcoming exams scheduled on your calendar.',
        recommendation: 'You currently have no pending exams. Maintain consistent class attendance and coursework revision.',
        prioritizedExams: [],
        isFallback: false
      }
    };
  }

  const formattedExams = upcomingExams.map((e, idx) => {
    return `${idx + 1}. "${e.title}" (${e.subjectName || e.subjectCode || 'Subject'}) - Date: ${e.exam_date} (${e.daysRemaining === 0 ? 'TODAY' : `in ${e.daysRemaining} days`}) | Status: ${e.examStatus} | Urgency: ${e.urgency} | Location: ${e.location || 'Hall A'} | Syllabus: ${e.syllabus || 'Full syllabus'}`;
  }).join('\n');

  const pendingAssignments = assignments.filter(a => a.status !== 'Completed' && a.deadlineStatus !== 'COMPLETED').map(a => {
    return `- "${a.title}" (${a.course || a.subjectName}) - Due: ${a.dueDate} (${a.daysRemaining === 0 ? 'DUE TODAY' : `in ${a.daysRemaining} days`}) [Urgency: ${a.urgency}]`;
  }).join('\n') || 'No pending assignments';

  const attendanceSummary = attendance.map(a => {
    return `- ${a.subject}: ${a.percentage}% (${a.status}) [Max miss: ${a.maxCanMiss ?? 0}, Must attend: ${a.minRequiredToAttend ?? 0}]`;
  }).join('\n') || 'All attendance normal';

  const promptText = `You are an expert AI Exam & Academic Advisor.
Analyze the following logged-in student's AUTHORITATIVE exam schedule, assignments, and attendance data to provide strategic, prioritized guidance.

STRICT RULE: Only reference the exact exams, dates, assignments, and attendance metrics provided below. Do NOT invent missing exams, subjects, or deadlines.

AUTHORITATIVE UPCOMING EXAMS:
${formattedExams}

RELATED PENDING ASSIGNMENTS:
${pendingAssignments}

ATTENDANCE STANDING:
${attendanceSummary}

Answer the following questions concisely and structurally:
1. Which exam should the student focus on FIRST?
2. How should they divide their daily study time between exam review and pending assignments?
3. What specific topics/syllabus items should they study TODAY?
4. Which assignments MUST be completed before the upcoming exams?
5. How should attendance risks affect their daily study priorities?
6. Strategic advice for managing multiple approaching exams.`;

  const fallback = generateFallbackExamAdvice(upcomingExams, assignments, attendance);

  if (!apiKey || apiKey === 'mock_key' || apiKey === 'your_gemini_api_key_here' || apiKey.trim() === '') {
    return { success: true, data: { ...fallback, isFallback: true } };
  }

  try {
    const ai = new GoogleGenAI({ apiKey });
    const modelsToTry = ['gemini-2.5-flash', 'gemini-2.0-flash', 'gemini-1.5-flash'];

    for (const modelName of modelsToTry) {
      try {
        const response = await ai.models.generateContent({
          model: modelName,
          contents: promptText
        });
        if (response && response.text) {
          return {
            success: true,
            data: {
              summary: `AI Exam Analysis complete for ${upcomingExams.length} upcoming exam(s).`,
              recommendation: response.text.trim(),
              prioritizedExams: upcomingExams,
              isFallback: false
            }
          };
        }
      } catch (err) {
        // try next model
      }
    }
  } catch (err) {
    console.error('[Gemini Exam Advisor Error]:', err.message || err);
  }

  return { success: true, data: { ...fallback, isFallback: true } };
}

function generateFallbackExamAdvice(upcomingExams, assignments, attendance) {
  const criticalExams = upcomingExams.filter(e => e.urgency === 'CRITICAL');
  const highExams = upcomingExams.filter(e => e.urgency === 'HIGH');

  let recommendation = '';
  if (criticalExams.length > 0) {
    recommendation = `🚨 **CRITICAL EXAM TODAY**: You have ${criticalExams.length} exam(s) scheduled TODAY ("${criticalExams[0].title}"). Review summary formulas and key concepts immediately. Focus all study energy on this exam.`;
  } else if (highExams.length > 0) {
    recommendation = `⚡ **HIGH EXAM PRIORITY**: Focus on "${highExams[0].title}" taking place in ${highExams[0].daysRemaining} day(s). Dedicate 70% of your daily study schedule to active recall and problem-solving for this syllabus: ${highExams[0].syllabus || 'Core topics'}.`;
  } else {
    recommendation = `📘 **UPCOMING EXAM REVISION**: Next exam is "${upcomingExams[0].title}" in ${upcomingExams[0].daysRemaining} days. Create 45-minute daily study blocks for key topics to build a robust preparation buffer.`;
  }

  return {
    summary: `Deterministic Exam Strategy for ${upcomingExams.length} upcoming exam(s).`,
    recommendation,
    prioritizedExams: upcomingExams
  };
}
