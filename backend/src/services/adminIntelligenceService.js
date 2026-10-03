import { GoogleGenAI } from '@google/genai';
import { config } from '../config/env.js';
import { getDB } from '../storage/db.js';
import { calculateAttendanceMetrics } from './attendanceService.js';
import { calculateAssignmentMetrics } from './assignmentService.js';
import { calculateExamMetrics } from './examService.js';
import { calculateSkillGaps } from './careerService.js';
import { analyzeInterviewWeakAreas } from './resumeInterviewService.js';

/**
 * Deterministically aggregates system-wide statistics for the Admin Dashboard Overview.
 */
export function calculateAdminDashboardStats() {
  const db = getDB();

  const students = db.students || [];
  const users = db.users || [];
  const attendance = db.attendance || [];
  const assignments = db.assignments || [];
  const exams = db.exams || [];
  const quizAttempts = db.quiz_attempts || [];
  const careerProfiles = db.career_profiles || [];
  const documents = db.documents || [];
  const documentChunks = db.document_chunks || [];

  // 1. STUDENTS
  const totalStudents = students.length;
  // Student is active if they have any attendance, assignment, exam, quiz attempt, or study session
  const activeStudentIds = new Set([
    ...attendance.map(a => a.student_id),
    ...assignments.map(a => a.student_id).filter(Boolean),
    ...exams.map(e => e.student_id).filter(Boolean),
    ...quizAttempts.map(q => q.student_id).filter(Boolean)
  ]);
  const activeStudents = activeStudentIds.size;

  // 2. ATTENDANCE
  const studentAttendanceMap = {};
  attendance.forEach(a => {
    if (!studentAttendanceMap[a.student_id]) {
      studentAttendanceMap[a.student_id] = { conducted: 0, attended: 0 };
    }
    studentAttendanceMap[a.student_id].conducted += Number(a.classes_conducted || 0);
    studentAttendanceMap[a.student_id].attended += Number(a.classes_attended || 0);
  });

  const studentsWithAttendanceCount = Object.keys(studentAttendanceMap).length;
  let totalAttendanceSum = 0;
  let belowTargetCount = 0;
  let atRiskCount = 0;

  Object.values(studentAttendanceMap).forEach(att => {
    const pct = att.conducted > 0 ? (att.attended / att.conducted) * 100 : 0;
    totalAttendanceSum += pct;
    if (pct < 75) {
      belowTargetCount += 1;
      atRiskCount += 1;
    } else if (pct < 80) {
      atRiskCount += 1;
    }
  });

  const averageAttendance = studentsWithAttendanceCount > 0
    ? Number((totalAttendanceSum / studentsWithAttendanceCount).toFixed(2))
    : 0;

  // 3. ASSIGNMENTS
  let pendingAssignments = 0;
  let completedAssignments = 0;
  let overdueAssignments = 0;
  let dueSoonAssignments = 0;

  assignments.forEach(a => {
    const m = calculateAssignmentMetrics(a);
    const isCompleted = (a.status || '').toLowerCase() === 'completed';
    if (isCompleted) {
      completedAssignments += 1;
    } else {
      pendingAssignments += 1;
      if (m.deadlineStatus === 'OVERDUE') overdueAssignments += 1;
      if (m.deadlineStatus === 'DUE_SOON' || m.deadlineStatus === 'DUE_TODAY') dueSoonAssignments += 1;
    }
  });

  // 4. EXAMS
  let upcomingExams = 0;
  let examsToday = 0;
  let completedExams = 0;

  exams.forEach(e => {
    const m = calculateExamMetrics(e);
    if (m.examStatus === 'COMPLETED') {
      completedExams += 1;
    } else if (m.examStatus === 'TODAY') {
      examsToday += 1;
      upcomingExams += 1;
    } else {
      upcomingExams += 1;
    }
  });

  // 5. QUIZZES
  const totalQuizAttempts = quizAttempts.length;
  let totalQuizScoreSum = 0;
  let weakQuizAttempts = 0;
  const studentQuizScores = {};

  quizAttempts.forEach(q => {
    const pct = Number(q.percentage || 0);
    totalQuizScoreSum += pct;
    if (pct < 60) weakQuizAttempts += 1;

    if (!studentQuizScores[q.student_id]) studentQuizScores[q.student_id] = [];
    studentQuizScores[q.student_id].push(pct);
  });

  const averageQuizScore = totalQuizAttempts > 0
    ? Number((totalQuizScoreSum / totalQuizAttempts).toFixed(2))
    : 0;

  let weakStudentsCount = 0;
  Object.values(studentQuizScores).forEach(scores => {
    const avg = scores.reduce((a, b) => a + b, 0) / scores.length;
    if (avg < 60) weakStudentsCount += 1;
  });

  // 6. CAREER
  const studentsWithCareerProfiles = careerProfiles.length;
  let studentsWithSkillGaps = 0;
  const skillGapFrequency = {};

  careerProfiles.forEach(p => {
    const gaps = calculateSkillGaps(p.skills || [], p.target_role || 'Full Stack Developer');
    if (gaps.gapCount > 0) studentsWithSkillGaps += 1;

    (gaps.missingList || []).forEach(skill => {
      skillGapFrequency[skill] = (skillGapFrequency[skill] || 0) + 1;
    });
  });

  const commonSkillGaps = Object.entries(skillGapFrequency)
    .map(([skill, count]) => ({ skill, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5);

  // 7. DOCUMENTS
  const totalDocuments = documents.length;
  const totalIndexedChunks = documentChunks.length;
  const indexingStatus = totalIndexedChunks > 0 ? 'Synchronized' : 'Pending';

  return {
    students: {
      totalStudents,
      activeStudents
    },
    attendance: {
      totalStudentsWithAttendance: studentsWithAttendanceCount,
      averageAttendance,
      studentsBelowAttendanceTarget: belowTargetCount,
      studentsAtRisk: atRiskCount
    },
    assignments: {
      totalAssignments: assignments.length,
      pendingAssignments,
      completedAssignments,
      overdueAssignments,
      dueSoonAssignments
    },
    exams: {
      totalExams: exams.length,
      upcomingExams,
      examsToday,
      completedExams
    },
    quizzes: {
      totalQuizAttempts,
      averageQuizScore,
      weakQuizAttempts,
      weakStudents: weakStudentsCount
    },
    career: {
      studentsWithCareerProfiles,
      studentsWithSkillGaps,
      commonSkillGaps
    },
    documents: {
      totalDocuments,
      totalIndexedChunks,
      embeddingProvider: 'Google Gemini text-embedding-004',
      indexingStatus
    }
  };
}

/**
 * Deterministically returns the student management list with academic summaries & risk indicators.
 */
export function getAdminStudentsList({ search = '', semester = '', department = '', attendanceRisk = '' } = {}) {
  const db = getDB();

  const students = db.students || [];
  const users = db.users || [];
  const attendance = db.attendance || [];
  const assignments = db.assignments || [];
  const exams = db.exams || [];
  const quizAttempts = db.quiz_attempts || [];
  const careerProfiles = db.career_profiles || [];

  const studentList = students.map(student => {
    const user = users.find(u => u.id === student.user_id) || {};

    // 1. Attendance aggregation
    const studentAtt = attendance.filter(a => a.student_id === student.id);
    let totalConducted = 0;
    let totalAttended = 0;
    studentAtt.forEach(a => {
      totalConducted += Number(a.classes_conducted || 0);
      totalAttended += Number(a.classes_attended || 0);
    });

    const attMetrics = calculateAttendanceMetrics({
      conducted: totalConducted,
      attended: totalAttended,
      targetPercentage: 75
    });

    // 2. Assignments
    const studentAsg = assignments.filter(a => a.student_id === student.id);
    let pendingAssignmentsCount = 0;
    let overdueAssignmentsCount = 0;
    studentAsg.forEach(a => {
      if ((a.status || '').toLowerCase() !== 'completed') {
        pendingAssignmentsCount += 1;
        const m = calculateAssignmentMetrics(a);
        if (m.deadlineStatus === 'OVERDUE') overdueAssignmentsCount += 1;
      }
    });

    // 3. Exams
    const studentExams = exams.filter(e => e.student_id === student.id);
    let upcomingExamsCount = 0;
    let examsTodayCount = 0;
    studentExams.forEach(e => {
      const m = calculateExamMetrics(e);
      if (m.examStatus !== 'COMPLETED') {
        upcomingExamsCount += 1;
        if (m.examStatus === 'TODAY') examsTodayCount += 1;
      }
    });

    // 4. Quizzes
    const studentQuizzes = quizAttempts.filter(q => q.student_id === student.id);
    const quizAttemptsCount = studentQuizzes.length;
    const averageQuizScore = quizAttemptsCount > 0
      ? Number((studentQuizzes.reduce((sum, q) => sum + Number(q.percentage || 0), 0) / quizAttemptsCount).toFixed(2))
      : null;

    // 5. Career Profile
    const career = careerProfiles.find(p => p.student_id === student.id);
    let targetRole = null;
    let skillCoverage = null;
    let gapCount = 0;

    if (career) {
      targetRole = career.target_role || 'Full Stack Developer';
      const gapAnalysis = calculateSkillGaps(career.skills || [], targetRole);
      skillCoverage = gapAnalysis.coveragePercentage;
      gapCount = gapAnalysis.gapCount;
    }

    // 6. Risk Indicators
    const riskIndicators = [];
    if (totalConducted > 0 && attMetrics.riskClassification === 'CRITICAL') {
      riskIndicators.push('ATTENDANCE_CRITICAL');
    } else if (totalConducted > 0 && attMetrics.riskClassification === 'WARNING') {
      riskIndicators.push('ATTENDANCE_WARNING');
    }

    if (overdueAssignmentsCount > 0) riskIndicators.push('OVERDUE_ASSIGNMENTS');
    if (examsTodayCount > 0) riskIndicators.push('EXAM_TODAY');
    if (averageQuizScore !== null && averageQuizScore < 60) riskIndicators.push('WEAK_QUIZ_PERFORMANCE');
    if (career && (gapCount >= 4 || (skillCoverage !== null && skillCoverage < 50))) {
      riskIndicators.push('CAREER_SKILL_GAPS');
    }

    // OMIT ALL SECRETS
    return {
      id: student.id,
      userId: user.id,
      name: user.name || 'Unknown Student',
      email: user.email || 'N/A',
      studentCode: student.student_code || 'N/A',
      semester: student.semester || 1,
      department: student.department || 'Computer Science & Engineering',
      gpa: student.gpa || 3.8,
      attendance: {
        conducted: totalConducted,
        attended: totalAttended,
        percentage: attMetrics.percentage,
        riskClassification: attMetrics.riskClassification,
        status: attMetrics.status
      },
      pendingAssignmentsCount,
      overdueAssignmentsCount,
      upcomingExamsCount,
      quizAttemptsCount,
      averageQuizScore,
      targetRole,
      skillCoverage,
      riskIndicators
    };
  });

  // Apply deterministic filtering
  return studentList.filter(s => {
    if (search && search.trim()) {
      const q = search.toLowerCase().trim();
      const match = s.name.toLowerCase().includes(q) ||
                    s.email.toLowerCase().includes(q) ||
                    s.studentCode.toLowerCase().includes(q) ||
                    s.department.toLowerCase().includes(q);
      if (!match) return false;
    }

    if (semester && String(s.semester) !== String(semester).trim()) {
      return false;
    }

    if (department && s.department.toLowerCase() !== department.toLowerCase().trim()) {
      return false;
    }

    if (attendanceRisk && s.attendance.riskClassification !== attendanceRisk.toUpperCase().trim()) {
      return false;
    }

    return true;
  });
}

/**
 * Returns full administrative drill-down overview for a specific student.
 */
export function getAdminStudentDetail(studentId) {
  const db = getDB();

  const student = (db.students || []).find(s => s.id === studentId || s.student_code === studentId);
  if (!student) return null;

  const user = (db.users || []).find(u => u.id === student.user_id) || {};
  const subjects = db.subjects || [];

  // Attendance Detail
  const studentAtt = (db.attendance || []).filter(a => a.student_id === student.id).map(a => {
    const sub = subjects.find(s => s.id === a.subject_id);
    const m = calculateAttendanceMetrics({
      conducted: a.classes_conducted,
      attended: a.classes_attended,
      targetPercentage: 75
    });
    return {
      subjectId: a.subject_id,
      subjectCode: sub?.code || 'CS',
      subjectName: sub?.name || 'Subject',
      conducted: a.classes_conducted,
      attended: a.classes_attended,
      percentage: m.percentage,
      riskClassification: m.riskClassification,
      status: m.status,
      maxCanMiss: m.maxCanMiss,
      minRequiredToAttend: m.minRequiredToAttend
    };
  });

  let totalConducted = 0;
  let totalAttended = 0;
  studentAtt.forEach(a => {
    totalConducted += a.conducted;
    totalAttended += a.attended;
  });
  const overallAttendance = calculateAttendanceMetrics({
    conducted: totalConducted,
    attended: totalAttended,
    targetPercentage: 75
  });

  // Assignments Detail
  const studentAsg = (db.assignments || [])
    .filter(a => a.student_id === student.id)
    .map(a => {
      const sub = subjects.find(s => s.id === a.subject_id);
      const m = calculateAssignmentMetrics(a);
      return {
        id: a.id,
        title: a.title,
        subject: sub?.name || a.course || 'Course',
        dueDate: a.dueDate || a.due_date,
        priority: a.priority || 'Medium',
        status: a.status || 'Pending',
        daysRemaining: m.daysRemaining,
        deadlineStatus: m.deadlineStatus,
        urgency: m.urgency
      };
    });

  // Exams Detail
  const studentExams = (db.exams || [])
    .filter(e => e.student_id === student.id)
    .map(e => {
      const sub = subjects.find(s => s.id === e.subject_id);
      const m = calculateExamMetrics(e);
      return {
        id: e.id,
        title: e.title,
        subject: sub?.name || 'Course',
        examDate: e.exam_date || e.examDate,
        startTime: e.start_time || '09:00 AM',
        location: e.location || 'Hall',
        syllabus: e.syllabus || '',
        daysRemaining: m.daysRemaining,
        examStatus: m.examStatus,
        urgency: m.urgency
      };
    });

  // Quiz Attempts Detail
  const studentQuizzes = (db.quiz_attempts || []).filter(q => q.student_id === student.id);
  const quizAvg = studentQuizzes.length > 0
    ? Number((studentQuizzes.reduce((sum, q) => sum + Number(q.percentage || 0), 0) / studentQuizzes.length).toFixed(2))
    : 0;

  // Career Profile & Skill Gaps
  const career = (db.career_profiles || []).find(p => p.student_id === student.id);
  let careerAnalysis = null;
  if (career) {
    const gaps = calculateSkillGaps(career.skills || [], career.target_role || 'Full Stack Developer');
    careerAnalysis = {
      careerGoal: career.career_goal,
      targetRole: career.target_role,
      domain: career.domain,
      skills: career.skills || [],
      skillGapAnalysis: gaps
    };
  }

  // Resumes
  const studentResumes = (db.resumes || [])
    .filter(r => r.student_id === student.id)
    .map(r => ({
      id: r.id,
      title: r.title,
      target_role: r.target_role,
      updated_at: r.updated_at || r.created_at,
      coveragePercentage: r.analysis?.coverage?.coveragePercentage ?? null
    }));

  // Interview History
  const studentPractices = (db.interview_practices || []).filter(p => p.student_id === student.id);
  const interviewWeakAnalysis = analyzeInterviewWeakAreas(studentPractices);

  return {
    profile: {
      id: student.id,
      userId: user.id,
      studentCode: student.student_code,
      name: user.name,
      email: user.email,
      semester: student.semester,
      department: student.department,
      gpa: student.gpa,
      createdAt: user.created_at
    },
    attendance: {
      overall: {
        conducted: totalConducted,
        attended: totalAttended,
        percentage: overallAttendance.percentage,
        targetPercentage: 75,
        riskClassification: overallAttendance.riskClassification,
        status: overallAttendance.status,
        maxCanMiss: overallAttendance.maxCanMiss,
        minRequiredToAttend: overallAttendance.minRequiredToAttend
      },
      subjects: studentAtt
    },
    assignments: {
      total: studentAsg.length,
      pending: studentAsg.filter(a => a.status !== 'Completed').length,
      overdue: studentAsg.filter(a => a.deadlineStatus === 'OVERDUE').length,
      list: studentAsg
    },
    exams: {
      total: studentExams.length,
      upcoming: studentExams.filter(e => e.examStatus !== 'COMPLETED').length,
      today: studentExams.filter(e => e.examStatus === 'TODAY').length,
      list: studentExams
    },
    quizzes: {
      totalAttempts: studentQuizzes.length,
      averageScore: quizAvg,
      attempts: studentQuizzes
    },
    career: careerAnalysis,
    resumes: studentResumes,
    interview: {
      totalAttempts: studentPractices.length,
      averageScore: interviewWeakAnalysis.average_score,
      weakAreas: interviewWeakAnalysis.weak_areas,
      history: studentPractices.slice(0, 5)
    }
  };
}

/**
 * Calculates academic risks categorized deterministically across all students.
 */
export function calculateAcademicRisks() {
  const students = getAdminStudentsList();

  const attendanceRisk = [];
  const assignmentRisk = [];
  const examRisk = [];
  const quizRisk = [];
  const careerGap = [];

  students.forEach(s => {
    // 1. Attendance Risk (< 75% or CRITICAL/WARNING)
    if (s.attendance.conducted > 0 && (s.attendance.percentage < 75 || s.attendance.riskClassification === 'CRITICAL')) {
      attendanceRisk.push({
        studentId: s.id,
        name: s.name,
        department: s.department,
        semester: s.semester,
        percentage: s.attendance.percentage,
        conducted: s.attendance.conducted,
        attended: s.attendance.attended,
        riskClassification: s.attendance.riskClassification
      });
    }

    // 2. Assignment Risk (Overdue or Due Soon)
    if (s.overdueAssignmentsCount > 0) {
      assignmentRisk.push({
        studentId: s.id,
        name: s.name,
        department: s.department,
        overdueCount: s.overdueAssignmentsCount,
        pendingCount: s.pendingAssignmentsCount
      });
    }

    // 3. Exam Risk (Exam Today or Upcoming <= 3 days)
    if (s.riskIndicators.includes('EXAM_TODAY') || s.riskIndicators.includes('EXAM_IMMINENT')) {
      examRisk.push({
        studentId: s.id,
        name: s.name,
        department: s.department,
        upcomingCount: s.upcomingExamsCount,
        isExamToday: s.riskIndicators.includes('EXAM_TODAY')
      });
    }

    // 4. Quiz Risk (Average < 60%)
    if (s.averageQuizScore !== null && s.averageQuizScore < 60) {
      quizRisk.push({
        studentId: s.id,
        name: s.name,
        department: s.department,
        averageScore: s.averageQuizScore,
        attemptsCount: s.quizAttemptsCount
      });
    }

    // 5. Career Skill Gap (Gaps identified or Coverage < 50%)
    if (s.riskIndicators.includes('CAREER_SKILL_GAPS')) {
      careerGap.push({
        studentId: s.id,
        name: s.name,
        targetRole: s.targetRole,
        skillCoverage: s.skillCoverage
      });
    }
  });

  return {
    attendanceRisk,
    assignmentRisk,
    examRisk,
    quizRisk,
    careerGap
  };
}

/**
 * Generates deterministic fallback insights summary when Gemini is offline or mock mode.
 */
function generateDeterministicFallbackInsights(stats = {}, risks = {}) {
  const safeStats = {
    students: { totalStudents: 0, activeStudents: 0, ...(stats?.students || {}) },
    attendance: { averageAttendance: 0, studentsBelowAttendanceTarget: 0, ...(stats?.attendance || {}) },
    assignments: { pendingAssignments: 0, overdueAssignments: 0, totalAssignments: 0, ...(stats?.assignments || {}) },
    exams: { upcomingExams: 0, examsToday: 0, ...(stats?.exams || {}) },
    career: { studentsWithSkillGaps: 0, commonSkillGaps: [], ...(stats?.career || {}) }
  };
  const safeRisks = {
    attendanceRisk: risks?.attendanceRisk || [],
    assignmentRisk: risks?.assignmentRisk || [],
    examRisk: risks?.examRisk || [],
    quizRisk: risks?.quizRisk || [],
    careerGap: risks?.careerGap || []
  };

  const observations = [
    `Enrollment status: ${safeStats.students.totalStudents} total students enrolled (${safeStats.students.activeStudents} active).`,
    `Campus-wide attendance average is ${safeStats.attendance.averageAttendance}%. ${safeStats.attendance.studentsBelowAttendanceTarget} student(s) fall below the 75% mandatory threshold.`,
    `Assignment load: ${safeStats.assignments.pendingAssignments} pending assignments with ${safeStats.assignments.overdueAssignments} overdue submission(s).`,
    `Examination readiness: ${safeStats.exams.upcomingExams} scheduled exam(s), with ${safeStats.exams.examsToday} exam(s) scheduled for today.`,
    `Technical career readiness: ${safeStats.career.studentsWithSkillGaps} student(s) have identified competency gaps in target roles.`
  ];

  const attention = [];
  if (safeRisks.attendanceRisk.length > 0) {
    attention.push(`${safeRisks.attendanceRisk.length} student(s) currently breach the 75% attendance policy and require immediate academic warning letters.`);
  }
  if (safeRisks.assignmentRisk.length > 0) {
    attention.push(`${safeRisks.assignmentRisk.length} student(s) have overdue assignment deadlines requiring departmental intervention.`);
  }
  if (safeRisks.examRisk.length > 0) {
    attention.push(`${safeRisks.examRisk.length} student(s) have exams scheduled today or within 3 days.`);
  }
  if (attention.length === 0) {
    attention.push('All enrolled students currently meet baseline academic engagement benchmarks.');
  }

  const actions = [
    'Issue automated notifications to students whose attendance is in CRITICAL warning status (<75%).',
    'Coordinate with course instructors regarding overdue assignments and offer tutoring assistance.',
    'Schedule mock technical interview sessions for students with high skill gaps in target engineering roles.',
    'Ensure RAG college policy documents remain up-to-date with current semester examination schedules.'
  ];

  return {
    keyObservations: observations,
    areasRequiringAttention: attention,
    notableAcademicPatterns: [
      `Course subject catalog contains ${safeStats.assignments.totalAssignments} practical project assessments.`,
      `Most common industry skill gaps across profiles: ${safeStats.career.commonSkillGaps.map(g => `${g.skill} (${g.count})`).join(', ') || 'None recorded'}.`
    ],
    suggestedAdministrativeActions: actions,
    caveatsAndLimitations: 'Deterministic summary generated directly from active database records without predictive extrapolation.',
    isAiGenerated: false
  };
}

/**
 * Generates AI Executive Insights using Gemini with strict factual constraints.
 */
export async function generateAdminInsightsAI({ dashboardStats, riskData }) {
  const fallback = generateDeterministicFallbackInsights(dashboardStats, riskData);

  const apiKey = process.env.GEMINI_API_KEY || config.geminiApiKey;
  if (!apiKey || apiKey === 'mock_key' || apiKey === 'your_gemini_api_key_here' || apiKey.trim() === '') {
    return fallback;
  }

  try {
    const ai = new GoogleGenAI({ apiKey });
    const promptText = `You are the Lead Academic Administrator & Intelligence Advisor for a university college copilot system.
Analyze the following AUTHORITATIVE campus academic statistics and risk metrics.

STRICT CONSTRAINTS & ANTI-HALLUCINATION RULES:
1. ONLY reference the exact numbers and categories provided in the JSON data below.
2. Do NOT invent student names, unlisted percentages, nonexistent trends, or fictitious departments.
3. Keep suggestions actionable, practical, and strictly advisory.
4. Output clean valid JSON matching the requested structure.

DETERMINISTIC CAMPUS STATISTICS:
${JSON.stringify(dashboardStats, null, 2)}

IDENTIFIED STUDENT RISK COUNTS:
- Attendance Risk (<75%): ${riskData.attendanceRisk.length} students
- Overdue Assignment Risk: ${riskData.assignmentRisk.length} students
- Imminent Exam Risk: ${riskData.examRisk.length} students
- Weak Quiz Performance (<60%): ${riskData.quizRisk.length} students
- Career Skill Gaps: ${riskData.careerGap.length} students

Return ONLY valid JSON with this exact structure:
{
  "keyObservations": ["point 1", "point 2", "point 3"],
  "areasRequiringAttention": ["point 1", "point 2"],
  "notableAcademicPatterns": ["point 1", "point 2"],
  "suggestedAdministrativeActions": ["action 1", "action 2", "action 3"],
  "caveatsAndLimitations": "Brief note on current data sample size or limitation."
}`;

    const modelsToTry = config.gemini?.generationModels || ['gemini-3.5-flash', 'gemini-3.5-flash-lite'];
    for (const modelName of modelsToTry) {
      try {
        const response = await ai.models.generateContent({
          model: modelName,
          contents: promptText
        });

        if (response && response.text) {
          const raw = response.text.trim();
          const match = raw.match(/\{[\s\S]*\}/);
          if (match) {
            const parsed = JSON.parse(match[0]);
            return {
              keyObservations: Array.isArray(parsed.keyObservations) ? parsed.keyObservations : fallback.keyObservations,
              areasRequiringAttention: Array.isArray(parsed.areasRequiringAttention) ? parsed.areasRequiringAttention : fallback.areasRequiringAttention,
              notableAcademicPatterns: Array.isArray(parsed.notableAcademicPatterns) ? parsed.notableAcademicPatterns : fallback.notableAcademicPatterns,
              suggestedAdministrativeActions: Array.isArray(parsed.suggestedAdministrativeActions) ? parsed.suggestedAdministrativeActions : fallback.suggestedAdministrativeActions,
              caveatsAndLimitations: parsed.caveatsAndLimitations || fallback.caveatsAndLimitations,
              isAiGenerated: true
            };
          }
        }
      } catch (err) {
        // try next model
      }
    }
  } catch (error) {
    console.error('[Admin AI Insights Error]:', error.message || error);
  }

  return fallback;
}

/**
 * Returns document management list with metadata and indexing status (no raw embedding vectors).
 */
export function getAdminDocumentsList() {
  const db = getDB();
  const documents = db.documents || [];
  const chunks = db.document_chunks || [];

  return documents.map(doc => {
    const docChunks = chunks.filter(c => c.document_id === doc.id);
    const hasEmbeddings = docChunks.length > 0 && docChunks.every(c => Array.isArray(c.embedding) && c.embedding.length > 0);

    return {
      id: doc.id,
      title: doc.title,
      fileName: doc.file_name,
      fileSize: doc.file_size,
      uploadedBy: doc.uploaded_by,
      pageCount: doc.page_count || 1,
      chunkCount: docChunks.length,
      createdAt: doc.created_at,
      embeddingStatus: hasEmbeddings ? 'Indexed' : (docChunks.length > 0 ? 'Partial' : 'Pending'),
      embeddingModel: 'text-embedding-004',
      provider: 'Google Gemini'
    };
  });
}
