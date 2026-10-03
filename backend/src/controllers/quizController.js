import { getDB, saveDB } from '../storage/db.js';
import { 
  generateQuizWithGemini, 
  evaluateQuizSubmission, 
  generateQuizAdvisorAdvice,
  validateQuizQuestion 
} from '../services/quizService.js';
import { buildStudentContext } from '../services/contextService.js';

export const generateQuiz = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { subject = 'CS 201', topic = 'Data Structures & Algorithms', difficulty = 'Medium', questionCount = 5 } = req.body;

    const studentContext = buildStudentContext(studentId);

    const aiResult = await generateQuizWithGemini({
      subject,
      topic,
      difficulty,
      questionCount,
      studentContext
    });

    if (!aiResult.success || !aiResult.questions || aiResult.questions.length === 0) {
      return res.status(422).json({ success: false, error: 'Failed to generate valid quiz questions' });
    }

    const db = getDB();
    if (!db.quizzes) db.quizzes = [];

    const quizId = 'qz-' + Date.now();
    const newQuiz = {
      id: quizId,
      student_id: studentId,
      subject,
      topic,
      difficulty,
      question_count: aiResult.questions.length,
      questions: aiResult.questions, // Store full questions including correct answers & explanations backend-side
      status: 'Pending',
      created_at: new Date().toISOString()
    };

    db.quizzes.unshift(newQuiz);
    saveDB(db);

    // ANSWER PROTECTION: Strip correct answers and explanations before sending public payload
    const publicQuestions = aiResult.questions.map(q => ({
      id: q.id,
      question: q.question,
      options: q.options
    }));

    res.status(201).json({
      success: true,
      data: {
        quizId: newQuiz.id,
        subject: newQuiz.subject,
        topic: newQuiz.topic,
        difficulty: newQuiz.difficulty,
        totalQuestions: publicQuestions.length,
        questions: publicQuestions,
        isFallback: aiResult.isFallback || false
      }
    });

  } catch (error) {
    next(error);
  }
};

export const submitQuiz = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const { answers = {}, rawQuestions } = req.body;
    const db = getDB();

    let targetQuiz = (db.quizzes || []).find(q => q.id === id);

    // If quiz not found by ID in db.quizzes, check if rawQuestions was provided (backward compatibility)
    let questionsToScore = [];
    if (targetQuiz) {
      // Ownership check: Student can only submit their own quiz
      if (targetQuiz.student_id !== studentId) {
        return res.status(403).json({ success: false, error: 'Access denied: You do not own this quiz' });
      }
      questionsToScore = targetQuiz.questions;
    } else if (Array.isArray(rawQuestions) && rawQuestions.length > 0) {
      questionsToScore = rawQuestions;
    } else {
      return res.status(404).json({ success: false, error: 'Quiz record not found' });
    }

    // Deterministic evaluation backend-side
    const evaluation = evaluateQuizSubmission(questionsToScore, answers);

    // Identify weak topics for this attempt
    const weakTopics = evaluation.percentage < 60 ? [targetQuiz?.topic || 'Topic Review'] : [];
    const recommendedTopics = evaluation.percentage < 80 
      ? [`${targetQuiz?.topic || 'Subject'} Concept Review`, 'Practice Problems'] 
      : ['Advanced Problem Solving'];

    const attemptId = 'qa-' + Date.now();
    const newAttempt = {
      id: attemptId,
      quiz_id: targetQuiz?.id || null,
      student_id: studentId,
      subject: targetQuiz?.subject || 'Subject',
      topic: targetQuiz?.topic || 'Topic',
      score: evaluation.score,
      total_questions: evaluation.totalQuestions,
      correct: evaluation.correct,
      incorrect: evaluation.incorrect,
      unanswered: evaluation.unanswered,
      percentage: evaluation.percentage,
      mastery_level: evaluation.masteryLevel,
      weak_topics: weakTopics,
      recommended_topics: recommendedTopics,
      created_at: new Date().toISOString()
    };

    if (!db.quiz_attempts) db.quiz_attempts = [];
    db.quiz_attempts.unshift(newAttempt);

    if (targetQuiz) {
      targetQuiz.status = 'Completed';
      targetQuiz.completed_at = new Date().toISOString();
      targetQuiz.score = evaluation.score;
      targetQuiz.percentage = evaluation.percentage;
    }

    saveDB(db);

    res.json({
      success: true,
      data: {
        attemptId: newAttempt.id,
        quizId: targetQuiz?.id || null,
        subject: targetQuiz?.subject || 'Subject',
        topic: targetQuiz?.topic || 'Topic',
        score: evaluation.score,
        totalQuestions: evaluation.totalQuestions,
        correctCount: evaluation.correct,
        incorrectCount: evaluation.incorrect,
        unansweredCount: evaluation.unanswered,
        percentage: evaluation.percentage,
        masteryLevel: evaluation.masteryLevel,
        results: evaluation.results, // Full question review with correct answers & explanations
        weakTopics,
        recommendedTopics,
        aiFeedback: evaluation.percentage >= 80 
          ? '🌟 Excellent performance! You demonstrate strong mastery of this topic.' 
          : evaluation.percentage >= 60 
          ? '👍 Good effort! Moderate understanding. Review the detailed explanations below.'
          : '📚 Weak topic identified (<60%). Focused revision recommended before exams.'
      }
    });

  } catch (error) {
    next(error);
  }
};

export const getQuizzes = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();

    const attempts = (db.quiz_attempts || []).filter(q => q.student_id === studentId);
    const quizzes = (db.quizzes || []).filter(q => q.student_id === studentId).map(q => ({
      id: q.id,
      subject: q.subject,
      topic: q.topic,
      difficulty: q.difficulty,
      questionCount: q.question_count,
      status: q.status,
      score: q.score ?? null,
      percentage: q.percentage ?? null,
      created_at: q.created_at
    }));

    res.json({
      success: true,
      data: {
        attempts,
        quizzes
      }
    });
  } catch (error) {
    next(error);
  }
};

export const getQuizById = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();

    const quiz = (db.quizzes || []).find(q => q.id === id);
    if (!quiz) {
      return res.status(404).json({ success: false, error: 'Quiz not found' });
    }

    // Authorization check: Student can only access their own quiz
    if (quiz.student_id !== studentId) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this quiz' });
    }

    // If completed, include full questions review; if pending, sanitize payload
    const isCompleted = quiz.status === 'Completed';
    const questionsPayload = isCompleted 
      ? quiz.questions 
      : quiz.questions.map(q => ({ id: q.id, question: q.question, options: q.options }));

    res.json({
      success: true,
      data: {
        ...quiz,
        questions: questionsPayload
      }
    });
  } catch (error) {
    next(error);
  }
};

export const deleteQuiz = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();

    const quiz = (db.quizzes || []).find(q => q.id === id);
    const attempt = (db.quiz_attempts || []).find(a => a.id === id || a.quiz_id === id);

    if (!quiz && !attempt) {
      return res.status(404).json({ success: false, error: 'Quiz or attempt not found' });
    }

    // Ownership check
    if (quiz && quiz.student_id !== studentId) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this quiz' });
    }
    if (attempt && attempt.student_id !== studentId) {
      return res.status(403).json({ success: false, error: 'Access denied: You do not own this quiz attempt' });
    }

    if (quiz) db.quizzes = db.quizzes.filter(q => q.id !== id);
    if (attempt) db.quiz_attempts = db.quiz_attempts.filter(a => a.id !== id && a.quiz_id !== id);

    saveDB(db);

    res.json({ success: true, message: 'Quiz deleted successfully' });
  } catch (error) {
    next(error);
  }
};

export const runQuizAdvisor = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();
    const attempts = (db.quiz_attempts || []).filter(q => q.student_id === studentId);
    const studentContext = buildStudentContext(studentId);

    const aiResult = await generateQuizAdvisorAdvice({
      attempts,
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
