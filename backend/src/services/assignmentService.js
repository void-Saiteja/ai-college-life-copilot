import { GoogleGenAI } from '@google/genai';
import { config } from '../config/env.js';

/**
 * Deterministically calculates days remaining, deadline status, and urgency.
 */
export function calculateAssignmentMetrics(assignment) {
  const statusRaw = (assignment.status || 'Pending').trim();
  const isCompleted = statusRaw.toLowerCase() === 'completed';

  if (isCompleted) {
    return {
      daysRemaining: 0,
      deadlineStatus: 'COMPLETED',
      urgency: 'COMPLETED'
    };
  }

  const dueDateStr = assignment.dueDate || assignment.due_date;
  if (!dueDateStr) {
    return {
      daysRemaining: 999,
      deadlineStatus: 'UPCOMING',
      urgency: 'LOW'
    };
  }

  // Parse YYYY-MM-DD in local time
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const [year, month, day] = dueDateStr.split('-').map(Number);
  const due = new Date(year, month - 1, day);
  due.setHours(0, 0, 0, 0);

  const diffTime = due.getTime() - today.getTime();
  const daysRemaining = Math.round(diffTime / (1000 * 60 * 60 * 24));

  // Deadline Status Logic
  let deadlineStatus = 'UPCOMING';
  if (daysRemaining < 0) {
    deadlineStatus = 'OVERDUE';
  } else if (daysRemaining === 0) {
    deadlineStatus = 'DUE_TODAY';
  } else if (daysRemaining <= 3) {
    deadlineStatus = 'DUE_SOON';
  } else {
    deadlineStatus = 'UPCOMING';
  }

  // Urgency / Priority Logic
  let urgency = 'LOW';
  if (daysRemaining <= 0) {
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
    deadlineStatus,
    urgency
  };
}

/**
 * Generates AI Deadline Prioritization using authoritative backend context.
 */
export async function generateAssignmentPrioritization({ assignments = [], studentContext = {} }) {
  const apiKey = process.env.GEMINI_API_KEY || config.geminiApiKey;

  const pending = assignments.filter(a => a.deadlineStatus !== 'COMPLETED' && a.status !== 'Completed');

  if (pending.length === 0) {
    return {
      success: true,
      data: {
        summary: '🎉 All caught up! You currently have no pending assignments.',
        recommendation: 'Use your free time for self-study, exam revision, or relaxation.',
        prioritizedTasks: [],
        isFallback: false
      }
    };
  }

  const formattedAssignments = pending.map((a, idx) => {
    return `${idx + 1}. "${a.title}" (${a.course || a.subject || 'Course'}) - Due: ${a.dueDate} (${a.daysRemaining < 0 ? `${Math.abs(a.daysRemaining)} days overdue` : a.daysRemaining === 0 ? 'DUE TODAY' : `${a.daysRemaining} days remaining`}) | Deadline Status: ${a.deadlineStatus} | Urgency: ${a.urgency} | Est. Hours: ${a.estimatedHours || 2}h`;
  }).join('\n');

  const exams = (studentContext.upcomingExams || []).map(e => `- ${e.title} (${e.subject}) on ${e.examDate}`).join('\n') || 'None scheduled';
  const attendance = (studentContext.attendance || []).map(a => `- ${a.subject}: ${a.percentage}% (${a.status})`).join('\n') || 'Normal';

  const promptText = `You are an expert AI Academic Deadline Advisor.
Analyze the following logged-in student's AUTHORITATIVE assignment list and academic context to provide actionable, prioritized advice.

STRICT RULE: Only reference the exact assignments listed below. Do NOT invent missing assignments, deadlines, or exam dates.

LOGGED-IN STUDENT PENDING ASSIGNMENTS:
${formattedAssignments}

UPCOMING EXAMS:
${exams}

ATTENDANCE STANDING:
${attendance}

Answer the following 5 questions concisely and structurally:
1. What should the student focus on FIRST?
2. WHY?
3. How should they divide their available study time?
4. Which assignments must be handled IMMEDIATELY?
5. What should they avoid postponing?`;

  const fallback = generateFallbackPrioritization(pending);

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
              summary: `AI Analysis complete for ${pending.length} pending assignment(s).`,
              recommendation: response.text.trim(),
              prioritizedTasks: pending,
              isFallback: false
            }
          };
        }
      } catch (err) {
        // try next model
      }
    }
  } catch (err) {
    console.error('[Gemini Assignment Advisor Error]:', err.message || err);
  }

  return { success: true, data: { ...fallback, isFallback: true } };
}

function generateFallbackPrioritization(pending) {
  const criticals = pending.filter(a => a.urgency === 'CRITICAL');
  const highs = pending.filter(a => a.urgency === 'HIGH');

  let recommendation = '';
  if (criticals.length > 0) {
    recommendation = `🚨 **IMMEDIATE ACTION REQUIRED**: You have ${criticals.length} CRITICAL assignment(s) (${criticals.map(c => `"${c.title}"`).join(', ')}) that are overdue or due today. Focus 100% of your current study session on completing these immediately before starting any new coursework.`;
  } else if (highs.length > 0) {
    recommendation = `⚡ **HIGH PRIORITY FOCUS**: Focus on "${highs[0].title}" due in ${highs[0].daysRemaining} day(s). Allocate 2-3 hours today to ensure completion before the deadline.`;
  } else {
    recommendation = `✅ **STABLE WORKLOAD**: All your pending assignments are upcoming. Plan to tackle "${pending[0].title}" early to maintain a healthy study buffer.`;
  }

  return {
    summary: `Deterministic Workload Analysis for ${pending.length} pending item(s).`,
    recommendation,
    prioritizedTasks: pending
  };
}
