import { GoogleGenAI } from '@google/genai';
import { config } from '../config/env.js';

/**
 * Validates a generated quiz question structure.
 */
export function validateQuizQuestion(q) {
  if (!q || typeof q !== 'object') return false;
  if (!q.question || typeof q.question !== 'string' || q.question.trim().length === 0) return false;
  if (!Array.isArray(q.options) || q.options.length < 2) return false;
  
  const answerIdx = Number(q.correctAnswer);
  if (isNaN(answerIdx) || answerIdx < 0 || answerIdx >= q.options.length) return false;
  
  return true;
}

/**
 * Generates an AI MCQ Quiz using Google Gemini with structured JSON validation.
 */
export async function generateQuizWithGemini({ subject = 'CS 201', topic = 'Data Structures', difficulty = 'Medium', questionCount = 5, studentContext = {} }) {
  const apiKey = process.env.GEMINI_API_KEY || config.geminiApiKey;
  const count = Math.max(1, Math.min(10, Number(questionCount || 5)));

  const upcomingExams = (studentContext.upcomingExams || []).map(e => `- ${e.title} (${e.subject}) in ${e.daysRemaining} days`).join('\n') || 'None';

  const promptText = `You are an expert AI Exam & Quiz Generator for college students.
Generate a high-quality, practical multiple-choice quiz (MCQ) for exam preparation.

QUIZ CONFIGURATION:
- Subject: ${subject}
- Topic/Unit: ${topic}
- Difficulty Level: ${difficulty}
- Number of Questions: ${count}

LOGGED-IN STUDENT CONTEXT:
- Upcoming Exams: ${upcomingExams}

STRICT JSON OUTPUT REQUIREMENT:
Return ONLY a raw valid JSON array (no markdown block, no conversational text) containing exactly ${count} question objects:
[
  {
    "id": 1,
    "question": "Clear and unambiguous multiple choice question text?",
    "options": ["Option A", "Option B", "Option C", "Option D"],
    "correctAnswer": 0,
    "explanation": "Detailed explanation of why Option A is correct and why other options are incorrect."
  }
]`;

  const fallbackQuestions = generateFallbackQuestions(subject, topic, count);

  if (!apiKey || apiKey === 'mock_key' || apiKey === 'your_gemini_api_key_here' || apiKey.trim() === '') {
    return { success: true, questions: fallbackQuestions, isFallback: true };
  }

  try {
    const ai = new GoogleGenAI({ apiKey });
    const modelsToTry = config.gemini?.generationModels || ['gemini-3.5-flash', 'gemini-3.5-flash-lite'];
    let rawText = '';

    for (const modelName of modelsToTry) {
      try {
        const response = await ai.models.generateContent({
          model: modelName,
          contents: promptText
        });
        if (response && response.text) {
          rawText = response.text;
          break;
        }
      } catch (err) {
        // try next model
      }
    }

    if (rawText) {
      const cleanJson = rawText.replace(/```json/gi, '').replace(/```/g, '').trim();
      const parsed = JSON.parse(cleanJson);

      if (Array.isArray(parsed) && parsed.length > 0) {
        // Validate each question
        const validQuestions = [];
        parsed.forEach((q, idx) => {
          if (validateQuizQuestion(q)) {
            validQuestions.push({
              id: idx + 1,
              question: q.question.trim(),
              options: q.options.map(opt => String(opt).trim()),
              correctAnswer: Number(q.correctAnswer),
              explanation: q.explanation ? String(q.explanation).trim() : 'Correct answer verified.'
            });
          }
        });

        if (validQuestions.length > 0) {
          return { success: true, questions: validQuestions, isFallback: false };
        }
      }
    }
  } catch (err) {
    console.error('[Gemini Quiz Service Error]:', err.message || err);
  }

  return { success: true, questions: fallbackQuestions, isFallback: true };
}

/**
 * Deterministically evaluates quiz answers.
 */
export function evaluateQuizSubmission(quizQuestions = [], studentAnswers = {}) {
  let correct = 0;
  let incorrect = 0;
  let unanswered = 0;

  const results = quizQuestions.map(q => {
    const selected = studentAnswers[q.id];
    const hasAnswered = selected !== undefined && selected !== null && selected !== '';
    const selectedIdx = hasAnswered ? Number(selected) : null;
    const isCorrect = hasAnswered && selectedIdx === q.correctAnswer;

    if (!hasAnswered) {
      unanswered += 1;
    } else if (isCorrect) {
      correct += 1;
    } else {
      incorrect += 1;
    }

    return {
      id: q.id,
      question: q.question,
      options: q.options,
      selectedAnswer: selectedIdx,
      correctAnswer: q.correctAnswer,
      isCorrect,
      explanation: q.explanation
    };
  });

  const totalQuestions = quizQuestions.length;
  const score = correct;
  const percentage = totalQuestions > 0 ? Number(((correct / totalQuestions) * 100).toFixed(2)) : 0;

  let masteryLevel = 'STRONG';
  if (percentage < 60) {
    masteryLevel = 'WEAK';
  } else if (percentage < 80) {
    masteryLevel = 'MODERATE';
  } else {
    masteryLevel = 'STRONG';
  }

  return {
    score,
    totalQuestions,
    correct,
    incorrect,
    unanswered,
    percentage,
    masteryLevel,
    results
  };
}

/**
 * Generates AI Quiz Advisory recommendations based on actual quiz attempts and student context.
 */
export async function generateQuizAdvisorAdvice({ attempts = [], studentContext = {} }) {
  const apiKey = process.env.GEMINI_API_KEY || config.geminiApiKey;

  if (attempts.length === 0) {
    return {
      success: true,
      data: {
        summary: 'No quiz performance history found.',
        recommendation: 'You have not taken any quizzes yet. Generate a topic quiz for your upcoming exams to establish a revision baseline.',
        weakTopics: [],
        strongTopics: [],
        isFallback: false
      }
    };
  }

  // Calculate weak and strong topics deterministically
  const topicScores = {};
  attempts.forEach(a => {
    const topicKey = `${a.subject || 'CS'} - ${a.topic || 'General'}`;
    if (!topicScores[topicKey]) {
      topicScores[topicKey] = [];
    }
    topicScores[topicKey].push(a.percentage);
  });

  const weakTopics = [];
  const strongTopics = [];

  Object.entries(topicScores).forEach(([t, scores]) => {
    const avg = scores.reduce((sum, s) => sum + s, 0) / scores.length;
    if (avg < 60) {
      weakTopics.push({ topic: t, avgScore: Number(avg.toFixed(1)) });
    } else if (avg >= 80) {
      strongTopics.push({ topic: t, avgScore: Number(avg.toFixed(1)) });
    }
  });

  const formattedAttempts = attempts.slice(0, 5).map(a => `- ${a.subject || 'CS'} (${a.topic}): ${a.percentage}% (${a.score}/${a.total_questions})`).join('\n');
  const upcomingExams = (studentContext.upcomingExams || []).map(e => `- ${e.title} (${e.subject}) in ${e.daysRemaining} days [Urgency: ${e.urgency}]`).join('\n') || 'None';

  const promptText = `You are an expert AI Quiz & Revision Advisor for college students.
Analyze the student's AUTHORITATIVE quiz attempt history and upcoming exam schedule.

STRICT RULE: Only reference actual quiz attempts and exams listed below. Do NOT invent fake scores or missing tests.

QUIZ ATTEMPT HISTORY:
${formattedAttempts}

DETERMINISTIC WEAK TOPICS (<60%):
${weakTopics.map(w => `- ${w.topic} (Avg: ${w.avgScore}%)`).join('\n') || 'None identified (<60%)'}

DETERMINISTIC STRONG TOPICS (>=80%):
${strongTopics.map(s => `- ${s.topic} (Avg: ${s.avgScore}%)`).join('\n') || 'None'}

UPCOMING EXAMS:
${upcomingExams}

Answer the following questions concisely:
1. Which topic should the student revise FIRST?
2. Why are they weak in this topic based on quiz performance?
3. What specific practice questions or concepts should they tackle next?
4. How should they align their quiz revision with upcoming exam deadlines?`;

  const fallback = generateFallbackAdvisorAdvice(attempts, weakTopics, strongTopics, studentContext);

  if (!apiKey || apiKey === 'mock_key' || apiKey === 'your_gemini_api_key_here' || apiKey.trim() === '') {
    return { success: true, data: { ...fallback, isFallback: true } };
  }

  try {
    const ai = new GoogleGenAI({ apiKey });
    const modelsToTry = config.gemini?.generationModels || ['gemini-3.5-flash', 'gemini-3.5-flash-lite'];

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
              summary: `AI Revision Strategy for ${attempts.length} quiz attempt(s).`,
              recommendation: response.text.trim(),
              weakTopics: weakTopics.map(w => w.topic),
              strongTopics: strongTopics.map(s => s.topic),
              isFallback: false
            }
          };
        }
      } catch (err) {
        // try next model
      }
    }
  } catch (err) {
    console.error('[Gemini Quiz Advisor Error]:', err.message || err);
  }

  return { success: true, data: { ...fallback, isFallback: true } };
}

function generateFallbackQuestions(subject, topic, count) {
  const sample = [
    {
      id: 1,
      question: `In ${topic}, what is the primary purpose of time complexity analysis?`,
      options: ['To measure exact execution time in milliseconds', 'To evaluate algorithm efficiency as input size grows', 'To count lines of code', 'To verify compiler version'],
      correctAnswer: 1,
      explanation: 'Time complexity analysis uses Asymptotic Notation (Big-O) to evaluate how resource consumption grows relative to input size N.'
    },
    {
      id: 2,
      question: `Which data structure operating principle applies to ${topic}?`,
      options: ['First In First Out (FIFO)', 'Last In First Out (LIFO)', 'Random Access Memory', 'Asymmetric Key Cryptography'],
      correctAnswer: 0,
      explanation: 'Standard queues and buffer streams follow First In First Out (FIFO) ordering.'
    },
    {
      id: 3,
      question: `What is the worst-case space complexity of storing N nodes in a binary tree?`,
      options: ['O(1)', 'O(log N)', 'O(N)', 'O(N^2)'],
      correctAnswer: 2,
      explanation: 'Storing N nodes requires memory proportional to N, resulting in O(N) space complexity.'
    },
    {
      id: 4,
      question: `Which strategy is most effective when preparing for an exam in ${subject}?`,
      options: ['Passive re-reading of notes', 'Active recall and timed MCQ practice', 'Cramming 1 hour before exam', 'Memorizing code without execution'],
      correctAnswer: 1,
      explanation: 'Active recall and timed self-testing produce significantly higher long-term retention and problem-solving readiness.'
    },
    {
      id: 5,
      question: `In computer science theory, what does a balanced search tree guarantee for search operations?`,
      options: ['O(1) time complexity', 'O(log N) time complexity', 'O(N) time complexity', 'O(N log N) time complexity'],
      correctAnswer: 1,
      explanation: 'Balanced search trees (like AVL or Red-Black trees) guarantee logarithmic O(log N) height, ensuring O(log N) search time.'
    }
  ];

  return sample.slice(0, count);
}

function generateFallbackAdvisorAdvice(attempts, weakTopics, strongTopics, studentContext) {
  let recommendation = '';
  if (weakTopics.length > 0) {
    recommendation = `🎯 **REVISION FOCUS**: Your quiz results indicate weak mastery (<60%) in **${weakTopics[0].topic}** (Avg: ${weakTopics[0].avgScore}%). We recommend generating a targeted 5-question quiz for this topic before your upcoming exams.`;
  } else if (strongTopics.length > 0) {
    recommendation = `🌟 **STRONG MASTERY**: You demonstrate solid performance (&ge;80%) in **${strongTopics[0].topic}**! Focus your upcoming study sessions on remaining core subjects.`;
  } else {
    recommendation = `📚 **BALANCED PROGRESS**: Your average quiz score across ${attempts.length} attempts is moderate. Take additional quizzes for approaching exam subjects to build high confidence.`;
  }

  return {
    summary: `Deterministic Performance Analysis for ${attempts.length} attempt(s).`,
    recommendation,
    weakTopics: weakTopics.map(w => w.topic),
    strongTopics: strongTopics.map(s => s.topic)
  };
}
