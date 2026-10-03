import { getDB } from '../storage/db.js';
import { calculateAssignmentMetrics } from './assignmentService.js';
import { calculateExamMetrics } from './examService.js';
import { calculateSkillGaps } from './careerService.js';

export function buildStudentContext(studentId = 'std-1') {
  const db = getDB();

  const student = db.students.find(s => s.id === studentId) || db.students[0];
  const user = db.users.find(u => u.id === student?.user_id) || db.users[1];

  const subjects = db.subjects || [];
  
  const studentAttendance = (db.attendance || []).filter(a => a.student_id === student?.id).map(a => {
    const sub = subjects.find(s => s.id === a.subject_id);
    const pct = a.classes_conducted > 0 ? Math.round((a.classes_attended / a.classes_conducted) * 100) : 0;
    return {
      subject: sub?.name || 'Subject',
      code: sub?.code || 'CS',
      conducted: a.classes_conducted,
      attended: a.classes_attended,
      percentage: pct,
      status: pct >= 75 ? 'Good Standing' : 'Low Attendance Warning'
    };
  });

  const upcomingAssignments = (db.assignments || [])
    .filter(a => (a.student_id === student?.id || !a.student_id) && a.status !== 'Completed')
    .map(a => {
      const sub = subjects.find(s => s.id === a.subject_id || s.code === a.course || s.name === a.course);
      const metrics = calculateAssignmentMetrics(a);
      return {
        id: a.id,
        title: a.title,
        subject: sub?.name || a.course || 'Course',
        dueDate: a.dueDate || a.due_date,
        priority: metrics.urgency,
        urgency: metrics.urgency,
        daysRemaining: metrics.daysRemaining,
        deadlineStatus: metrics.deadlineStatus,
        estimatedHours: a.estimatedHours || a.estimated_hours || 2
      };
    });

  const upcomingExams = (db.exams || [])
    .filter(e => (e.student_id === student?.id || !e.student_id) && e.status !== 'Completed')
    .map(e => {
      const sub = subjects.find(s => s.id === e.subject_id);
      const metrics = calculateExamMetrics(e);
      return {
        id: e.id,
        title: e.title,
        subject: sub?.name || 'Course',
        subjectCode: sub?.code || 'CS',
        examDate: e.exam_date || e.examDate,
        startTime: e.start_time || e.startTime,
        location: e.location,
        syllabus: e.syllabus,
        daysRemaining: metrics.daysRemaining,
        examStatus: metrics.examStatus,
        urgency: metrics.urgency
      };
    });

  const activePlan = (db.study_plans || []).find(p => p.student_id === student?.id && p.status === 'Active');
  const sessions = activePlan ? (db.study_sessions || []).filter(s => s.plan_id === activePlan.id) : [];

  const studentAttempts = (db.quiz_attempts || []).filter(q => q.student_id === student?.id);
  const recentQuiz = studentAttempts.length > 0 ? studentAttempts[0] : null;

  // Compute weak topics (<60%) across quiz attempts
  const topicScores = {};
  studentAttempts.forEach(a => {
    const tKey = `${a.subject || 'CS'} - ${a.topic || 'General'}`;
    if (!topicScores[tKey]) topicScores[tKey] = [];
    topicScores[tKey].push(a.percentage);
  });

  const weakTopics = [];
  Object.entries(topicScores).forEach(([t, scores]) => {
    const avg = scores.reduce((sum, s) => sum + s, 0) / scores.length;
    if (avg < 60) weakTopics.push(t);
  });

  // Career Profile Summary
  const careerProfile = (db.career_profiles || []).find(p => p.student_id === student?.id);
  let careerProfileSummary = null;
  if (careerProfile) {
    const gapAnalysis = calculateSkillGaps(careerProfile.skills || [], careerProfile.target_role || 'Full Stack Developer');
    careerProfileSummary = {
      careerGoal: careerProfile.career_goal,
      targetRole: careerProfile.target_role,
      coveragePercentage: gapAnalysis.coveragePercentage,
      missingSkills: gapAnalysis.missingList,
      partialSkills: gapAnalysis.partialList
    };
  }

  return {
    student: {
      id: student?.id,
      name: user?.name || 'Alex Mercer',
      email: user?.email || 'alex@student.edu',
      semester: student?.semester || 4,
      department: student?.department || 'Computer Science & Engineering',
      gpa: student?.gpa || 3.85
    },
    subjects,
    attendance: studentAttendance,
    upcomingAssignments,
    upcomingExams,
    studyPlan: {
      plan: activePlan,
      sessions
    },
    recentQuizScore: recentQuiz ? `${recentQuiz.percentage}% in ${recentQuiz.topic}` : 'No quiz attempts recorded',
    quizHistorySummary: {
      totalAttempts: studentAttempts.length,
      weakTopics,
      recentAttempt: recentQuiz
    },
    careerProfileSummary
  };
}
