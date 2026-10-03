import { GoogleGenAI } from '@google/genai';
import { config } from '../config/env.js';

/**
 * Backend-controlled Target Role Skill Catalog.
 */
export const ROLE_SKILL_CATALOG = {
  'Full Stack Developer': ['JavaScript', 'React', 'Node.js', 'REST APIs', 'SQL', 'Git', 'Docker'],
  'Frontend Developer': ['JavaScript', 'React', 'HTML', 'CSS', 'TypeScript', 'Tailwind', 'Git'],
  'Backend Developer': ['Node.js', 'Express', 'SQL', 'PostgreSQL', 'REST APIs', 'Docker', 'Git'],
  'Data Analyst': ['Python', 'SQL', 'Statistics', 'Pandas', 'Data Visualization', 'Excel'],
  'Data Scientist': ['Python', 'SQL', 'Statistics', 'Pandas', 'Machine Learning', 'Data Visualization'],
  'Machine Learning Engineer': ['Python', 'Machine Learning', 'Deep Learning', 'PyTorch', 'SQL', 'Docker'],
  'AI Engineer': ['Python', 'Machine Learning', 'Deep Learning', 'NLP', 'APIs', 'Docker']
};

export const VALID_PROFICIENCIES = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];

/**
 * Validates proficiency string.
 */
export function validateProficiency(prof) {
  if (!prof || typeof prof !== 'string') return false;
  return VALID_PROFICIENCIES.includes(prof.trim().toUpperCase());
}

/**
 * Deterministically analyzes skill gaps between student profile and target role requirements.
 */
export function calculateSkillGaps(studentSkills = [], targetRole = 'Full Stack Developer') {
  const requiredSkills = ROLE_SKILL_CATALOG[targetRole] || ROLE_SKILL_CATALOG['Full Stack Developer'];

  // Map student skills for case-insensitive lookup
  const studentSkillMap = {};
  studentSkills.forEach(s => {
    if (s && s.skill) {
      studentSkillMap[s.skill.toLowerCase().trim()] = (s.proficiency || 'BEGINNER').toUpperCase().trim();
    }
  });

  const skillAnalysis = [];
  let strongCount = 0;
  let partialCount = 0;
  let gapCount = 0;

  const strongList = [];
  const partialList = [];
  const missingList = [];

  requiredSkills.forEach(reqSkill => {
    const sLower = reqSkill.toLowerCase().trim();
    const studentProf = studentSkillMap[sLower];

    let classification = 'GAP';

    if (!studentProf) {
      classification = 'GAP';
      gapCount += 1;
      missingList.push(reqSkill);
    } else if (studentProf === 'ADVANCED' || studentProf === 'INTERMEDIATE') {
      classification = 'STRONG';
      strongCount += 1;
      strongList.push(reqSkill);
    } else if (studentProf === 'BEGINNER') {
      classification = 'PARTIAL';
      partialCount += 1;
      partialList.push(reqSkill);
    } else {
      classification = 'GAP';
      gapCount += 1;
      missingList.push(reqSkill);
    }

    skillAnalysis.push({
      skill: reqSkill,
      studentProficiency: studentProf || 'NONE',
      classification
    });
  });

  const totalRequired = requiredSkills.length;
  const coveragePercentage = totalRequired > 0 
    ? Number((((strongCount + partialCount) / totalRequired) * 100).toFixed(2)) 
    : 0;

  return {
    targetRole,
    totalRequired,
    strongCount,
    partialCount,
    gapCount,
    coveragePercentage,
    strongList,
    partialList,
    missingList,
    skillAnalysis
  };
}

/**
 * Generates AI Career Guidance based on student profile and deterministic skill gaps.
 */
export async function generateCareerAdvice({ careerProfile, skillGapAnalysis, studentContext = {} }) {
  const apiKey = process.env.GEMINI_API_KEY || config.geminiApiKey;

  if (!careerProfile) {
    return {
      success: false,
      error: 'Career profile required',
      recommendation: 'Please setup your career profile and skills first.'
    };
  }

  const { targetRole, coveragePercentage, missingList, partialList, strongList } = skillGapAnalysis;
  const weakQuizTopics = studentContext.quizHistorySummary?.weakTopics || [];

  const promptText = `You are an expert AI Career & Skill Advisor for college software engineers.
Analyze the logged-in student's AUTHORITATIVE career profile, skill gap calculations, and quiz performance.

STRICT RULE: Only reference actual skills, target roles, and quiz topics provided below. Do NOT invent fake projects, skills, or job offers.

CAREER PROFILE:
- Goal: ${careerProfile.career_goal || targetRole}
- Target Role: ${targetRole}
- Domain: ${careerProfile.domain || 'Software Development'}
- Current Skill Coverage: ${coveragePercentage}%

DETERMINISTIC SKILL GAP ANALYSIS:
- Strong Skills (&ge;Intermediate): ${strongList.join(', ') || 'None'}
- Partial Skills (Beginner): ${partialList.join(', ') || 'None'}
- Missing Critical Skills (Gaps): ${missingList.join(', ') || 'None'}

ACADEMIC WEAK TOPICS:
${weakQuizTopics.join(', ') || 'None (<60%)'}

Answer the following questions concisely:
1. What technical skills should the student focus on learning NEXT?
2. Which missing skill gaps are most critical for their role as ${targetRole}?
3. How can they leverage their existing strong skills (${strongList.slice(0, 3).join(', ')})?
4. Recommended practical project idea incorporating their missing skills.`;

  const fallback = generateFallbackCareerAdvice(skillGapAnalysis, careerProfile);

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
              summary: `AI Career Strategy for ${targetRole} (${coveragePercentage}% coverage).`,
              recommendation: response.text.trim(),
              skillGapAnalysis,
              isFallback: false
            }
          };
        }
      } catch (err) {
        // try next model
      }
    }
  } catch (err) {
    console.error('[Gemini Career Advisor Error]:', err.message || err);
  }

  return { success: true, data: { ...fallback, isFallback: true } };
}

/**
 * Generates a structured learning roadmap for closing identified skill gaps.
 */
export async function generateLearningRoadmap({ careerProfile, skillGapAnalysis }) {
  const apiKey = process.env.GEMINI_API_KEY || config.geminiApiKey;

  const { targetRole, missingList, partialList } = skillGapAnalysis;
  const gapsToClose = [...missingList, ...partialList];

  const fallbackRoadmap = generateFallbackRoadmap(targetRole, missingList, partialList);

  if (gapsToClose.length === 0) {
    return {
      success: true,
      data: {
        summary: `🎉 High Readiness! You meet all core skill requirements for ${targetRole}.`,
        phases: [
          {
            phase: 'Phase 1: Master-level Refinement',
            skills: ['Advanced System Architecture', 'Performance Optimization'],
            focus: 'Build full-stack open-source projects & practice mock system design interviews.',
            effort: '3-4 weeks'
          }
        ],
        isFallback: false
      }
    };
  }

  const promptText = `You are an expert AI Career Roadmap Architect.
Given a student's target role and deterministic skill gaps, construct a 3-phase structured learning roadmap.

STRICT RULE: Do NOT invent fake course URLs or external job offers. Focus purely on technical learning goals.

TARGET ROLE: ${targetRole}
IDENTIFIED SKILL GAPS TO CLOSE: ${gapsToClose.join(', ')}

STRICT JSON OUTPUT REQUIREMENT:
Return ONLY a valid JSON object (no markdown, no extra text):
{
  "summary": "Short 2-sentence summary of the roadmap strategy for ${targetRole}.",
  "phases": [
    {
      "phase": "Phase 1: Foundations",
      "skills": ["${gapsToClose[0] || 'Core Skill'}"],
      "priority": "HIGH",
      "focus": "Key concepts and hands-on exercises for ${gapsToClose[0] || 'Core Skill'}.",
      "effort": "2-3 weeks"
    }
  ]
}`;

  if (!apiKey || apiKey === 'mock_key' || apiKey === 'your_gemini_api_key_here' || apiKey.trim() === '') {
    return { success: true, data: { ...fallbackRoadmap, isFallback: true } };
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
      if (parsed && Array.isArray(parsed.phases) && parsed.phases.length > 0) {
        return { success: true, data: { ...parsed, isFallback: false } };
      }
    }
  } catch (err) {
    console.error('[Gemini Career Roadmap Error]:', err.message || err);
  }

  return { success: true, data: { ...fallbackRoadmap, isFallback: true } };
}

function generateFallbackCareerAdvice(skillGapAnalysis, careerProfile) {
  const { targetRole, missingList, partialList, coveragePercentage } = skillGapAnalysis;

  let recommendation = '';
  if (missingList.length > 0) {
    recommendation = `🎯 **SKILL GAP PRIORITIZATION**: For your target role as **${targetRole}**, your top priority should be acquiring **${missingList[0]}** and **${missingList[1] || missingList[0]}**. Current skill coverage is ${coveragePercentage}%.`;
  } else if (partialList.length > 0) {
    recommendation = `⚡ **PROFICIENCY UPGRADE**: You have basic exposure to **${partialList.join(', ')}**. Advance these skills from Beginner to Intermediate to improve role readiness.`;
  } else {
    recommendation = `🌟 **HIGH READINESS**: You meet the skill requirements for ${targetRole}! Focus on building comprehensive portfolio projects.`;
  }

  return {
    summary: `Deterministic Skill Gap Analysis for ${targetRole}.`,
    recommendation,
    skillGapAnalysis
  };
}

function generateFallbackRoadmap(targetRole, missingList, partialList) {
  const gaps = [...missingList, ...partialList];

  const phase1Skills = gaps.slice(0, 2);
  const phase2Skills = gaps.slice(2, 4);
  const phase3Skills = gaps.slice(4);

  const phases = [];

  if (phase1Skills.length > 0) {
    phases.push({
      phase: 'Phase 1: High Priority Foundations',
      skills: phase1Skills,
      priority: 'HIGH',
      focus: `Master fundamentals and practical exercises in ${phase1Skills.join(' and ')}.`,
      effort: '2-3 weeks'
    });
  }

  if (phase2Skills.length > 0) {
    phases.push({
      phase: 'Phase 2: Core Competencies',
      skills: phase2Skills,
      priority: 'MEDIUM',
      focus: `Build API & database features using ${phase2Skills.join(' and ')}.`,
      effort: '3-4 weeks'
    });
  }

  if (phase3Skills.length > 0) {
    phases.push({
      phase: 'Phase 3: Integration & Deployment',
      skills: phase3Skills,
      priority: 'MEDIUM',
      focus: `Integrate ${phase3Skills.join(' and ')} into full-stack project portfolio.`,
      effort: '2-3 weeks'
    });
  }

  return {
    summary: `Deterministic 3-Phase Learning Roadmap for ${targetRole}.`,
    phases
  };
}
