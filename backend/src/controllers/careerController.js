import { getDB, saveDB } from '../storage/db.js';
import { 
  ROLE_SKILL_CATALOG, 
  VALID_PROFICIENCIES, 
  validateProficiency, 
  calculateSkillGaps, 
  generateCareerAdvice, 
  generateLearningRoadmap 
} from '../services/careerService.js';
import { buildStudentContext } from '../services/contextService.js';
import {
  VALID_INTERVIEW_CATEGORIES,
  VALID_INTERVIEW_DIFFICULTIES,
  analyzeResumeWithAI,
  generateInterviewQuestionAI,
  evaluateInterviewAnswerAI,
  analyzeInterviewWeakAreas
} from '../services/resumeInterviewService.js';

export const getCareerProfile = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();
    const profile = (db.career_profiles || []).find(p => p.student_id === studentId);

    if (!profile) {
      return res.json({
        success: true,
        data: null,
        message: 'No career profile found for student. Please create a profile.'
      });
    }

    const targetRole = profile.target_role || 'Full Stack Developer';
    const skillGapAnalysis = calculateSkillGaps(profile.skills || [], targetRole);

    res.json({
      success: true,
      data: {
        profile,
        skillGapAnalysis
      }
    });
  } catch (error) {
    next(error);
  }
};

export const updateCareerProfile = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { career_goal, target_role, domain, skills, interests, experience_projects } = req.body;

    // Validate target_role if provided
    if (target_role && !ROLE_SKILL_CATALOG[target_role]) {
      return res.status(400).json({
        success: false,
        error: `Invalid target role '${target_role}'. Supported roles: ${Object.keys(ROLE_SKILL_CATALOG).join(', ')}`
      });
    }

    // Validate skills & proficiencies if provided
    if (skills && Array.isArray(skills)) {
      for (const s of skills) {
        if (!s.skill || typeof s.skill !== 'string' || !s.skill.trim()) {
          return res.status(400).json({ success: false, error: 'Skill name is required for all skills' });
        }
        if (s.proficiency && !validateProficiency(s.proficiency)) {
          return res.status(400).json({
            success: false,
            error: `Invalid proficiency level '${s.proficiency}'. Valid levels: ${VALID_PROFICIENCIES.join(', ')}`
          });
        }
      }
    }

    const db = getDB();
    if (!db.career_profiles) db.career_profiles = [];

    let index = db.career_profiles.findIndex(p => p.student_id === studentId);

    const now = new Date().toISOString();
    let updatedProfile;

    if (index === -1) {
      updatedProfile = {
        id: 'cp-' + Date.now(),
        student_id: studentId,
        career_goal: career_goal || 'Become a Full Stack Software Engineer',
        target_role: target_role || 'Full Stack Developer',
        domain: domain || 'Software Development',
        skills: (skills || []).map(s => ({ skill: s.skill.trim(), proficiency: (s.proficiency || 'BEGINNER').toUpperCase().trim() })),
        interests: interests || [],
        experience_projects: experience_projects || [],
        created_at: now,
        updated_at: now
      };
      db.career_profiles.unshift(updatedProfile);
    } else {
      const existing = db.career_profiles[index];
      updatedProfile = {
        ...existing,
        career_goal: career_goal !== undefined ? career_goal : existing.career_goal,
        target_role: target_role !== undefined ? target_role : existing.target_role,
        domain: domain !== undefined ? domain : existing.domain,
        skills: skills !== undefined 
          ? skills.map(s => ({ skill: s.skill.trim(), proficiency: (s.proficiency || 'BEGINNER').toUpperCase().trim() })) 
          : existing.skills,
        interests: interests !== undefined ? interests : existing.interests,
        experience_projects: experience_projects !== undefined ? experience_projects : existing.experience_projects,
        updated_at: now
      };
      db.career_profiles[index] = updatedProfile;
    }

    saveDB(db);

    const skillGapAnalysis = calculateSkillGaps(updatedProfile.skills, updatedProfile.target_role);

    res.status(200).json({
      success: true,
      data: {
        profile: updatedProfile,
        skillGapAnalysis
      }
    });

  } catch (error) {
    next(error);
  }
};

export const addSkill = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { skill, proficiency = 'BEGINNER' } = req.body;

    if (!skill || typeof skill !== 'string' || !skill.trim()) {
      return res.status(400).json({ success: false, error: 'Skill name is required' });
    }

    if (!validateProficiency(proficiency)) {
      return res.status(400).json({
        success: false,
        error: `Invalid proficiency level '${proficiency}'. Valid levels: ${VALID_PROFICIENCIES.join(', ')}`
      });
    }

    const db = getDB();
    if (!db.career_profiles) db.career_profiles = [];

    let profile = db.career_profiles.find(p => p.student_id === studentId);

    if (!profile) {
      profile = {
        id: 'cp-' + Date.now(),
        student_id: studentId,
        career_goal: 'Become a Software Engineer',
        target_role: 'Full Stack Developer',
        domain: 'Software Development',
        skills: [],
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      };
      db.career_profiles.unshift(profile);
    }

    // Check duplicate skill entry
    const existingIndex = profile.skills.findIndex(s => s.skill.toLowerCase().trim() === skill.toLowerCase().trim());
    if (existingIndex !== -1) {
      return res.status(409).json({ success: false, error: `Skill '${skill}' already exists in your career profile` });
    }

    profile.skills.push({
      skill: skill.trim(),
      proficiency: proficiency.toUpperCase().trim()
    });
    profile.updated_at = new Date().toISOString();

    saveDB(db);

    const skillGapAnalysis = calculateSkillGaps(profile.skills, profile.target_role);

    res.status(201).json({
      success: true,
      data: {
        profile,
        skillGapAnalysis
      }
    });

  } catch (error) {
    next(error);
  }
};

export const updateSkill = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { skillName } = req.params;
    const { proficiency } = req.body;

    if (!validateProficiency(proficiency)) {
      return res.status(400).json({
        success: false,
        error: `Invalid proficiency level '${proficiency}'. Valid levels: ${VALID_PROFICIENCIES.join(', ')}`
      });
    }

    const db = getDB();
    const profile = (db.career_profiles || []).find(p => p.student_id === studentId);

    if (!profile) {
      return res.status(404).json({ success: false, error: 'Career profile not found' });
    }

    const skillIdx = profile.skills.findIndex(s => s.skill.toLowerCase().trim() === skillName.toLowerCase().trim());
    if (skillIdx === -1) {
      return res.status(404).json({ success: false, error: `Skill '${skillName}' not found in profile` });
    }

    profile.skills[skillIdx].proficiency = proficiency.toUpperCase().trim();
    profile.updated_at = new Date().toISOString();

    saveDB(db);

    const skillGapAnalysis = calculateSkillGaps(profile.skills, profile.target_role);

    res.json({
      success: true,
      data: {
        profile,
        skillGapAnalysis
      }
    });
  } catch (error) {
    next(error);
  }
};

export const deleteSkill = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { skillName } = req.params;
    const db = getDB();
    const profile = (db.career_profiles || []).find(p => p.student_id === studentId);

    if (!profile) {
      return res.status(404).json({ success: false, error: 'Career profile not found' });
    }

    const initialLength = profile.skills.length;
    profile.skills = profile.skills.filter(s => s.skill.toLowerCase().trim() !== skillName.toLowerCase().trim());

    if (profile.skills.length === initialLength) {
      return res.status(404).json({ success: false, error: `Skill '${skillName}' not found in profile` });
    }

    profile.updated_at = new Date().toISOString();
    saveDB(db);

    const skillGapAnalysis = calculateSkillGaps(profile.skills, profile.target_role);

    res.json({
      success: true,
      data: {
        profile,
        skillGapAnalysis
      }
    });
  } catch (error) {
    next(error);
  }
};

export const runCareerAdvisor = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();
    const profile = (db.career_profiles || []).find(p => p.student_id === studentId);

    if (!profile) {
      return res.status(400).json({
        success: false,
        error: 'Career profile required. Please setup your target role and skills first.'
      });
    }

    const targetRole = profile.target_role || 'Full Stack Developer';
    const skillGapAnalysis = calculateSkillGaps(profile.skills || [], targetRole);
    const studentContext = buildStudentContext(studentId);

    const aiResult = await generateCareerAdvice({
      careerProfile: profile,
      skillGapAnalysis,
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

export const runLearningRoadmap = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();
    const profile = (db.career_profiles || []).find(p => p.student_id === studentId);

    if (!profile) {
      return res.status(400).json({
        success: false,
        error: 'Career profile required. Please setup your target role and skills first.'
      });
    }

    const targetRole = profile.target_role || 'Full Stack Developer';
    const skillGapAnalysis = calculateSkillGaps(profile.skills || [], targetRole);

    const aiResult = await generateLearningRoadmap({
      careerProfile: profile,
      skillGapAnalysis
    });

    res.json({
      success: true,
      data: aiResult.data
    });
  } catch (error) {
    next(error);
  }
};

// Preserve existing resume analysis & interview practice methods
export const analyzeResume = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }
    const { resumeText = '', targetRole = 'Full Stack Developer' } = req.body;
    if (!resumeText.trim()) {
      return res.status(400).json({ success: false, error: 'Resume text is required' });
    }
    const skillGapAnalysis = calculateSkillGaps([], targetRole);
    res.json({ success: true, data: { targetRole, skillGapAnalysis } });
  } catch (error) {
    next(error);
  }
};

export const practiceInterview = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }
    const { targetRole = 'Full Stack Developer', question, answer } = req.body;
    if (!answer) {
      return res.status(400).json({ success: false, error: 'Interview answer is required' });
    }
    res.json({
      success: true,
      data: {
        targetRole,
        question: question || 'Explain your technical approach.',
        evaluatedAnswer: answer,
        clarityScore: 85,
        feedback: '✅ Great technical response!',
        followUpQuestion: 'How would your solution scale if load increased 10x?'
      }
    });
  } catch (error) {
    next(error);
  }
};

// ==========================================
// RESUME CRUD & INTELLIGENCE
// ==========================================

export const getResumes = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();
    const resumes = (db.resumes || []).filter(r => r.student_id === studentId);

    res.json({
      success: true,
      data: resumes
    });
  } catch (error) {
    next(error);
  }
};

export const createResume = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { title, resume_text, target_role } = req.body;

    // Validation
    if (!resume_text || typeof resume_text !== 'string' || !resume_text.trim()) {
      return res.status(400).json({ success: false, error: 'Resume text is required and cannot be empty' });
    }

    if (resume_text.length > 50000) {
      return res.status(400).json({ success: false, error: 'Resume text exceeds maximum length of 50,000 characters' });
    }

    if (target_role && !ROLE_SKILL_CATALOG[target_role]) {
      return res.status(400).json({
        success: false,
        error: `Invalid target role '${target_role}'. Supported roles: ${Object.keys(ROLE_SKILL_CATALOG).join(', ')}`
      });
    }

    const db = getDB();
    if (!db.resumes) db.resumes = [];

    const profile = (db.career_profiles || []).find(p => p.student_id === studentId);
    const assignedRole = target_role || profile?.target_role || 'Full Stack Developer';
    const assignedTitle = (title && typeof title === 'string' && title.trim())
      ? title.trim()
      : `Resume Version (${new Date().toLocaleDateString()})`;

    const newResume = {
      id: 'res-' + Date.now() + '-' + Math.random().toString(36).substr(2, 6),
      student_id: studentId,
      title: assignedTitle,
      resume_text: resume_text.trim(),
      target_role: assignedRole,
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
      analysis: null
    };

    db.resumes.unshift(newResume);
    saveDB(db);

    res.status(201).json({
      success: true,
      data: newResume
    });
  } catch (error) {
    next(error);
  }
};

export const getResumeById = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();
    const resume = (db.resumes || []).find(r => r.id === id);

    if (!resume) {
      return res.status(404).json({ success: false, error: 'Resume not found' });
    }

    if (resume.student_id !== studentId) {
      return res.status(403).json({ success: false, error: 'Forbidden: Access denied to resume' });
    }

    res.json({
      success: true,
      data: resume
    });
  } catch (error) {
    next(error);
  }
};

export const updateResume = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();
    const resume = (db.resumes || []).find(r => r.id === id);

    if (!resume) {
      return res.status(404).json({ success: false, error: 'Resume not found' });
    }

    if (resume.student_id !== studentId) {
      return res.status(403).json({ success: false, error: 'Forbidden: Access denied to resume' });
    }

    const { title, resume_text, target_role } = req.body;

    if (resume_text !== undefined) {
      if (typeof resume_text !== 'string' || !resume_text.trim()) {
        return res.status(400).json({ success: false, error: 'Resume text cannot be empty' });
      }
      if (resume_text.length > 50000) {
        return res.status(400).json({ success: false, error: 'Resume text exceeds maximum length of 50,000 characters' });
      }
      resume.resume_text = resume_text.trim();
    }

    if (target_role !== undefined) {
      if (!ROLE_SKILL_CATALOG[target_role]) {
        return res.status(400).json({
          success: false,
          error: `Invalid target role '${target_role}'. Supported roles: ${Object.keys(ROLE_SKILL_CATALOG).join(', ')}`
        });
      }
      resume.target_role = target_role;
    }

    if (title !== undefined && typeof title === 'string' && title.trim()) {
      resume.title = title.trim();
    }

    resume.updated_at = new Date().toISOString();
    saveDB(db);

    res.json({
      success: true,
      data: resume
    });
  } catch (error) {
    next(error);
  }
};

export const deleteResume = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();
    const resume = (db.resumes || []).find(r => r.id === id);

    if (!resume) {
      return res.status(404).json({ success: false, error: 'Resume not found' });
    }

    if (resume.student_id !== studentId) {
      return res.status(403).json({ success: false, error: 'Forbidden: Access denied to resume' });
    }

    db.resumes = db.resumes.filter(r => r.id !== id);
    saveDB(db);

    res.json({
      success: true,
      message: 'Resume deleted successfully'
    });
  } catch (error) {
    next(error);
  }
};

export const analyzeResumeById = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const db = getDB();
    const resume = (db.resumes || []).find(r => r.id === id);

    if (!resume) {
      return res.status(404).json({ success: false, error: 'Resume not found' });
    }

    if (resume.student_id !== studentId) {
      return res.status(403).json({ success: false, error: 'Forbidden: Access denied to resume' });
    }

    const profile = (db.career_profiles || []).find(p => p.student_id === studentId);
    const targetRole = resume.target_role || profile?.target_role || 'Full Stack Developer';
    const skillGapAnalysis = calculateSkillGaps(profile?.skills || [], targetRole);

    const analysisResult = await analyzeResumeWithAI({
      resumeText: resume.resume_text,
      targetRole,
      careerProfile: profile,
      skillGapAnalysis
    });

    resume.analysis = analysisResult.data;
    resume.updated_at = new Date().toISOString();
    saveDB(db);

    res.json({
      success: true,
      data: resume.analysis
    });
  } catch (error) {
    next(error);
  }
};

// ==========================================
// INTERVIEW QUESTION GENERATION & PRACTICE
// ==========================================

export const generateInterviewQuestion = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { targetRole, category = 'TECHNICAL', difficulty = 'MEDIUM', skill } = req.body;

    if (category && !VALID_INTERVIEW_CATEGORIES.includes(category.toUpperCase())) {
      return res.status(400).json({
        success: false,
        error: `Invalid interview category '${category}'. Valid categories: ${VALID_INTERVIEW_CATEGORIES.join(', ')}`
      });
    }

    if (difficulty && !VALID_INTERVIEW_DIFFICULTIES.includes(difficulty.toUpperCase())) {
      return res.status(400).json({
        success: false,
        error: `Invalid difficulty '${difficulty}'. Valid difficulties: ${VALID_INTERVIEW_DIFFICULTIES.join(', ')}`
      });
    }

    if (targetRole && !ROLE_SKILL_CATALOG[targetRole]) {
      return res.status(400).json({
        success: false,
        error: `Invalid target role '${targetRole}'. Supported roles: ${Object.keys(ROLE_SKILL_CATALOG).join(', ')}`
      });
    }

    const db = getDB();
    const profile = (db.career_profiles || []).find(p => p.student_id === studentId);
    const assignedRole = targetRole || profile?.target_role || 'Full Stack Developer';
    const skillGapAnalysis = calculateSkillGaps(profile?.skills || [], assignedRole);

    const generated = await generateInterviewQuestionAI({
      targetRole: assignedRole,
      category,
      difficulty,
      skill,
      careerProfile: profile,
      skillGapAnalysis
    });

    // Validate generated question structure
    if (!generated || !generated.question || typeof generated.question !== 'string' || generated.question.trim().length < 5) {
      return res.status(422).json({ success: false, error: 'Malformed interview question generated' });
    }

    if (!db.interview_questions) db.interview_questions = [];

    const newQuestion = {
      id: 'iq-' + Date.now() + '-' + Math.random().toString(36).substr(2, 6),
      student_id: studentId,
      question: generated.question.trim(),
      category: generated.category,
      difficulty: generated.difficulty,
      target_skill: generated.target_skill,
      target_role: assignedRole,
      preparation_tips: generated.preparation_tips || '',
      isAiGenerated: !!generated.isAiGenerated,
      created_at: new Date().toISOString()
    };

    db.interview_questions.unshift(newQuestion);
    saveDB(db);

    res.status(201).json({
      success: true,
      data: newQuestion
    });
  } catch (error) {
    next(error);
  }
};

export const submitInterviewAnswer = async (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const { id } = req.params;
    const { answer } = req.body;

    const db = getDB();
    const question = (db.interview_questions || []).find(q => q.id === id);

    if (!question) {
      return res.status(404).json({ success: false, error: 'Interview question not found' });
    }

    if (question.student_id !== studentId) {
      return res.status(403).json({ success: false, error: 'Forbidden: Access denied to interview question' });
    }

    if (!answer || typeof answer !== 'string' || !answer.trim()) {
      return res.status(400).json({ success: false, error: 'Interview answer is required' });
    }

    if (answer.trim().length < 5) {
      return res.status(400).json({ success: false, error: 'Interview answer must be at least 5 characters long' });
    }

    if (answer.length > 10000) {
      return res.status(400).json({ success: false, error: 'Interview answer exceeds maximum length of 10,000 characters' });
    }

    const evaluation = await evaluateInterviewAnswerAI({
      question: question.question,
      answer: answer.trim(),
      category: question.category,
      difficulty: question.difficulty,
      targetSkill: question.target_skill,
      targetRole: question.target_role
    });

    if (!db.interview_practices) db.interview_practices = [];

    const practiceRecord = {
      id: 'ip-' + Date.now() + '-' + Math.random().toString(36).substr(2, 6),
      student_id: studentId,
      question_id: question.id,
      question: question.question,
      category: question.category,
      difficulty: question.difficulty,
      target_skill: question.target_skill,
      target_role: question.target_role,
      answer: answer.trim(),
      scores: evaluation.scores,
      feedback: evaluation.feedback,
      created_at: new Date().toISOString()
    };

    db.interview_practices.unshift(practiceRecord);
    saveDB(db);

    res.status(201).json({
      success: true,
      data: practiceRecord
    });
  } catch (error) {
    next(error);
  }
};

export const getInterviewHistory = (req, res, next) => {
  try {
    const studentId = req.user?.studentId;
    if (!studentId) {
      return res.status(401).json({ success: false, error: 'Unauthorized: Student authentication required' });
    }

    const db = getDB();
    const studentPractices = (db.interview_practices || []).filter(p => p.student_id === studentId);
    const weakAreasAnalysis = analyzeInterviewWeakAreas(studentPractices);

    res.json({
      success: true,
      data: {
        history: studentPractices,
        ...weakAreasAnalysis
      }
    });
  } catch (error) {
    next(error);
  }
};

