import { GoogleGenAI } from '@google/genai';
import { config } from '../config/env.js';
import { ROLE_SKILL_CATALOG } from './careerService.js';

export const VALID_INTERVIEW_CATEGORIES = ['TECHNICAL', 'BEHAVIORAL', 'PROJECT', 'ROLE_SPECIFIC'];
export const VALID_INTERVIEW_DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'];

// Master technical skills vocabulary for deterministic resume scanning
export const KNOWN_TECH_SKILLS = [
  'JavaScript', 'TypeScript', 'Python', 'Java', 'C++', 'C#', 'Go', 'Rust', 'PHP', 'Ruby', 'Swift', 'Kotlin',
  'React', 'Vue', 'Angular', 'Next.js', 'Node.js', 'Express', 'Django', 'Flask', 'FastAPI', 'Spring Boot',
  'HTML', 'CSS', 'Tailwind', 'Bootstrap', 'Sass', 'Redux', 'GraphQL',
  'SQL', 'PostgreSQL', 'MySQL', 'MongoDB', 'Redis', 'SQLite', 'Oracle',
  'REST APIs', 'APIs', 'Microservices', 'Docker', 'Kubernetes', 'AWS', 'Azure', 'GCP', 'Linux', 'Git', 'CI/CD',
  'Machine Learning', 'Deep Learning', 'PyTorch', 'TensorFlow', 'NLP', 'Computer Vision', 'Data Visualization',
  'Pandas', 'NumPy', 'Scikit-Learn', 'Statistics', 'Excel', 'Tableau', 'PowerBI'
];

/**
 * Extracts skills deterministically from resume text using regex word matching.
 * Returns labeled array of detected skills.
 */
export function extractSkillsFromResume(resumeText = '') {
  if (!resumeText || typeof resumeText !== 'string') return [];

  const detected = [];
  const textLower = resumeText.toLowerCase();

  // Combine known skills with all skills in ROLE_SKILL_CATALOG
  const allCatalogSkills = new Set(KNOWN_TECH_SKILLS);
  Object.values(ROLE_SKILL_CATALOG).forEach(skills => {
    skills.forEach(s => allCatalogSkills.add(s));
  });

  for (const skill of allCatalogSkills) {
    // Escape special regex characters like +, ., etc.
    const escaped = skill.replace(/[-/\\^$*+?.()|[\]{}]/g, '\\$&');
    let regex;
    if (skill === 'C++') {
      regex = /(?:^|\s|\W)c\+\+(?:$|\s|\W)/i;
    } else if (skill === 'C#') {
      regex = /(?:^|\s|\W)c#(?:$|\s|\W)/i;
    } else if (skill.toLowerCase() === 'rest apis' || skill.toLowerCase() === 'apis') {
      regex = /\b(rest(ful)?\s*apis?|apis?)\b/i;
    } else if (skill.toLowerCase() === 'node.js') {
      regex = /\bnode(\.js)?\b/i;
    } else if (skill.toLowerCase() === 'react') {
      regex = /\breact(\.js)?\b/i;
    } else {
      regex = new RegExp(`(^|[^a-zA-Z0-9_])${escaped}([^a-zA-Z0-9_]|$)`, 'i');
    }

    if (regex.test(textLower)) {
      detected.push({
        skill,
        status: 'DETECTED_IN_RESUME'
      });
    }
  }

  return detected;
}

/**
 * Calculates deterministic resume coverage metric against a target role.
 * Formula: (matched_required_skills / total_required_skills) * 100
 */
export function calculateResumeCoverage(detectedSkills = [], targetRole = 'Full Stack Developer') {
  const requiredSkills = ROLE_SKILL_CATALOG[targetRole] || ROLE_SKILL_CATALOG['Full Stack Developer'];
  const totalRequired = requiredSkills.length;

  const detectedSkillNames = new Set(
    detectedSkills.map(d => (typeof d === 'string' ? d : d.skill).toLowerCase().trim())
  );

  const matchedSkills = [];
  const missingSkills = [];

  requiredSkills.forEach(req => {
    if (detectedSkillNames.has(req.toLowerCase().trim())) {
      matchedSkills.push(req);
    } else {
      missingSkills.push(req);
    }
  });

  const matchedCount = matchedSkills.length;
  const coveragePercentage = totalRequired > 0 
    ? Number(((matchedCount / totalRequired) * 100).toFixed(2)) 
    : 0;

  return {
    targetRole,
    totalRequired,
    matchedCount,
    missingCount: missingSkills.length,
    coveragePercentage,
    matchedSkills,
    missingSkills,
    coverageFormula: '(detected_required_skills / total_required_skills) * 100'
  };
}

/**
 * Generates deterministic fallback resume advice when Gemini is unavailable.
 */
function generateFallbackResumeAdvice({ targetRole, coverage, detectedSkills, careerProfile }) {
  const { matchedSkills, missingSkills, coveragePercentage } = coverage;

  const strengths = matchedSkills.length > 0
    ? matchedSkills.map(s => `Strong representation of required competency '${s}' in your resume.`)
    : ['Resume structure contains readable experience and profile sections.'];

  const improvements = missingSkills.length > 0
    ? missingSkills.map(s => `No explicit evidence detected for required role skill '${s}'. Consider adding coursework or projects demonstrating '${s}'.`)
    : ['Solid coverage of core role skills. Focus on quantifying accomplishments and business impact.'];

  const suggestions = [
    `Target Role Alignment: Emphasize practical experience aligned with ${targetRole} requirements.`,
    missingSkills.length > 0 
      ? `Skill Clarification: If you have practical experience with ${missingSkills.slice(0, 3).join(', ')}, explicitly list them with project context.`
      : 'Highlight architectural decisions and performance optimizations.',
    'Impact & Metrics: Add quantifiable outcomes (e.g. % performance increase, query speedup, user scale) only where you have factual project data.',
    'Section Clarity: Ensure distinct sections for Summary, Technical Skills, Projects, and Education.'
  ];

  return {
    documentedFacts: {
      targetRole,
      detectedSkillsCount: detectedSkills.length,
      coveragePercentage,
      matchedRoleSkills: matchedSkills,
      missingRoleSkills: missingSkills
    },
    aiSuggestions: {
      strengths,
      improvements,
      actionableSuggestions: suggestions,
      isAiGenerated: false,
      disclaimer: 'Deterministic resume analysis based on role skill catalog and verified text parsing.'
    }
  };
}

/**
 * Analyzes resume using deterministic metrics + Gemini qualitative advisor.
 */
export async function analyzeResumeWithAI({ resumeText, targetRole = 'Full Stack Developer', careerProfile = null, skillGapAnalysis = null }) {
  // 1. Deterministic Extraction & Metric Calculation
  const detectedSkills = extractSkillsFromResume(resumeText);
  const coverage = calculateResumeCoverage(detectedSkills, targetRole);

  const fallback = generateFallbackResumeAdvice({
    targetRole,
    coverage,
    detectedSkills,
    careerProfile
  });

  const apiKey = process.env.GEMINI_API_KEY || config.geminiApiKey;
  if (!apiKey || apiKey === 'mock_key' || apiKey === 'your_gemini_api_key_here' || apiKey.trim() === '') {
    return {
      success: true,
      data: {
        targetRole,
        coverage,
        detectedSkills,
        careerSkillGaps: skillGapAnalysis,
        ...fallback
      }
    };
  }

  try {
    const ai = new GoogleGenAI({ apiKey });
    const promptText = `You are an expert Technical Resume Reviewer and Career Coach.
Analyze the following student resume for the target role: "${targetRole}".

CRITICAL INSTRUCTIONS & ANTI-HALLUCINATION RULES:
1. Do NOT invent fake projects, fake internships, fake companies, fake degrees, or fake metrics.
2. Only suggest improvements based on the student's actual written resume text.
3. Recommend adding skills or metrics ONLY IF the student genuinely possesses that experience.
4. Clearly provide constructive feedback.

DETERMINISTIC ANALYSIS CONTEXT:
- Target Role: ${targetRole}
- Required Skills: ${(ROLE_SKILL_CATALOG[targetRole] || []).join(', ')}
- Skills Detected in Resume: ${coverage.matchedSkills.join(', ') || 'None'}
- Missing Required Skills: ${coverage.missingSkills.join(', ') || 'None'}
- Deterministic Coverage: ${coverage.coveragePercentage}%

STUDENT RESUME TEXT:
"""
${resumeText.slice(0, 4000)}
"""

Provide your review as clean JSON with this exact structure:
{
  "strengths": ["bullet point 1", "bullet point 2"],
  "improvements": ["bullet point 1", "bullet point 2"],
  "actionableSuggestions": [
    "actionable suggestion 1",
    "actionable suggestion 2",
    "actionable suggestion 3"
  ]
}`;

    const modelsToTry = ['gemini-2.5-flash', 'gemini-2.0-flash', 'gemini-1.5-flash'];
    for (const modelName of modelsToTry) {
      try {
        const response = await ai.models.generateContent({
          model: modelName,
          contents: promptText
        });

        if (response && response.text) {
          const raw = response.text.trim();
          const jsonMatch = raw.match(/\{[\s\S]*\}/);
          if (jsonMatch) {
            const parsed = JSON.parse(jsonMatch[0]);
            return {
              success: true,
              data: {
                targetRole,
                coverage,
                detectedSkills,
                careerSkillGaps: skillGapAnalysis,
                documentedFacts: fallback.documentedFacts,
                aiSuggestions: {
                  strengths: Array.isArray(parsed.strengths) ? parsed.strengths : fallback.aiSuggestions.strengths,
                  improvements: Array.isArray(parsed.improvements) ? parsed.improvements : fallback.aiSuggestions.improvements,
                  actionableSuggestions: Array.isArray(parsed.actionableSuggestions) ? parsed.actionableSuggestions : fallback.aiSuggestions.actionableSuggestions,
                  isAiGenerated: true,
                  disclaimer: 'AI-generated qualitative suggestions. Verify all suggestions against your genuine academic and project experience.'
                }
              }
            };
          }
        }
      } catch (err) {
        // try next model
      }
    }
  } catch (error) {
    console.error('[Resume AI Analysis Error]:', error.message || error);
  }

  return {
    success: true,
    data: {
      targetRole,
      coverage,
      detectedSkills,
      careerSkillGaps: skillGapAnalysis,
      ...fallback
    }
  };
}

// Curated Fallback Interview Question Bank for 100% Reliability
const INTERVIEW_QUESTION_BANK = {
  TECHNICAL: {
    EASY: [
      { skill: 'JavaScript', question: 'Explain the difference between let, const, and var in modern JavaScript.', tips: 'Discuss scoping rules and hoisting.' },
      { skill: 'SQL', question: 'What is the difference between WHERE and HAVING clauses in SQL queries?', tips: 'Mention aggregation functions.' },
      { skill: 'React', question: 'What are React hooks, and how does the useState hook manage component state?', tips: 'Explain functional components and state immutability.' },
      { skill: 'Docker', question: 'What is the primary difference between a Docker image and a Docker container?', tips: 'Images are immutable templates; containers are running instances.' },
      { skill: 'Git', question: 'Explain the purpose of git branch and how you resolve a merge conflict.', tips: 'Mention git checkout / switch and merge markers.' }
    ],
    MEDIUM: [
      { skill: 'JavaScript', question: 'How does the JavaScript Event Loop handle asynchronous callbacks and promises (Microtasks vs Macrotasks)?', tips: 'Explain Call Stack, Web APIs, Microtask Queue, and Callback Queue.' },
      { skill: 'Node.js', question: 'How does Node.js handle concurrent I/O operations despite having a single-threaded event loop?', tips: 'Discuss libuv, worker threads, and non-blocking I/O.' },
      { skill: 'SQL', question: 'Explain database indexing: how do B-Trees optimize query lookups, and when might an index degrade performance?', tips: 'Discuss search time complexity O(log n) vs insert/update overhead.' },
      { skill: 'React', question: 'Explain the React virtual DOM reconciliation algorithm and how keys help optimize list rendering.', tips: 'Explain tree diffing and identity persistence.' },
      { skill: 'Docker', question: 'How would you write a multi-stage Dockerfile to minimize production image size for a Node.js or React application?', tips: 'Discuss build stage vs runtime stage.' },
      { skill: 'REST APIs', question: 'What are idempotent HTTP methods, and why are PUT and DELETE considered idempotent while POST is not?', tips: 'Explain the definition of idempotence and side effects.' }
    ],
    HARD: [
      { skill: 'Node.js', question: 'Design a high-throughput rate limiter middleware in Express/Node.js using Redis sliding windows or token buckets.', tips: 'Discuss race conditions, memory bounds, and atomic operations.' },
      { skill: 'SQL', question: 'How do database transaction isolation levels (Read Committed, Repeatable Read, Serializable) prevent dirty reads, non-repeatable reads, and phantom reads?', tips: 'Discuss MVCC and row/table locks.' },
      { skill: 'Docker', question: 'Explain container networking modes (bridge, host, overlay) and how Docker manages container-to-container communication with DNS and iptables.', tips: 'Discuss virtual ethernet pairs, network namespaces, and port mapping.' }
    ]
  },
  BEHAVIORAL: {
    EASY: [
      { skill: 'Teamwork', question: 'Tell me about a time you collaborated with peers on an academic software project. How did you divide responsibilities?', tips: 'Use the STAR format: Situation, Task, Action, Result.' },
      { skill: 'Communication', question: 'How do you approach explaining a technical concept to a non-technical teammate or stakeholder?', tips: 'Mention analogies, diagrams, and active listening.' }
    ],
    MEDIUM: [
      { skill: 'Conflict Resolution', question: 'Describe a situation where you disagreed with a team member about a technical architecture decision. How did you resolve it constructively?', tips: 'Focus on evaluating trade-offs, objective data, and consensus.' },
      { skill: 'Time Management', question: 'How do you prioritize multiple competing academic deadlines when an assignment and an exam overlap?', tips: 'Mention impact prioritization, time-boxing, and proactive communication.' }
    ],
    HARD: [
      { skill: 'Leadership', question: 'Describe a scenario where a critical project was falling significantly behind schedule. What concrete steps did you take to triage and deliver successfully?', tips: 'Discuss scope reduction, prioritization, and team coordination under pressure.' }
    ]
  },
  PROJECT: {
    EASY: [
      { skill: 'Full Stack', question: 'Walk me through the architecture of a full-stack web project you built. What technologies did you choose and why?', tips: 'Highlight frontend, backend API, and database choices.' }
    ],
    MEDIUM: [
      { skill: 'Database Design', question: 'Describe a project where you designed the database schema. How did you decide between relational (SQL) and document (NoSQL) storage?', tips: 'Discuss schema consistency, relationship complexity, and query patterns.' }
    ],
    HARD: [
      { skill: 'System Scaling', question: 'If your main college project suddenly received 10,000 concurrent requests per second, what bottlenecks would arise first and how would you resolve them?', tips: 'Discuss caching, connection pooling, horizontal scaling, and CDN.' }
    ]
  },
  ROLE_SPECIFIC: {
    EASY: [
      { skill: 'Web Development', question: 'As a junior developer, what steps do you take when troubleshooting a bug reported in production?', tips: 'Mention error logs, reproducible test cases, and git bisect.' }
    ],
    MEDIUM: [
      { skill: 'Full Stack Developer', question: 'As a Full Stack Developer, how do you enforce secure authentication and data authorization across both the frontend client and backend API?', tips: 'Mention JWTs, HttpOnly cookies, RBAC, and input sanitization.' }
    ],
    HARD: [
      { skill: 'Architecture', question: 'For your target role, how do you balance rapid feature delivery with code quality, automated test coverage, and technical debt management?', tips: 'Discuss CI/CD, modular architecture, and automated testing.' }
    ]
  }
};

/**
 * Generates an interview question using Gemini or fallback bank.
 */
export async function generateInterviewQuestionAI({
  targetRole = 'Full Stack Developer',
  category = 'TECHNICAL',
  difficulty = 'MEDIUM',
  skill = null,
  careerProfile = null,
  skillGapAnalysis = null
}) {
  const normCategory = VALID_INTERVIEW_CATEGORIES.includes(category?.toUpperCase())
    ? category.toUpperCase()
    : 'TECHNICAL';
  const normDifficulty = VALID_INTERVIEW_DIFFICULTIES.includes(difficulty?.toUpperCase())
    ? difficulty.toUpperCase()
    : 'MEDIUM';

  // Determine target skill if not explicitly provided
  let targetSkill = skill;
  if (!targetSkill) {
    if (skillGapAnalysis?.missingList && skillGapAnalysis.missingList.length > 0) {
      targetSkill = skillGapAnalysis.missingList[0];
    } else if (skillGapAnalysis?.partialList && skillGapAnalysis.partialList.length > 0) {
      targetSkill = skillGapAnalysis.partialList[0];
    } else {
      const roleSkills = ROLE_SKILL_CATALOG[targetRole] || ROLE_SKILL_CATALOG['Full Stack Developer'];
      targetSkill = roleSkills[0] || 'Software Engineering';
    }
  }

  // Get deterministic fallback question
  const fallbackList = INTERVIEW_QUESTION_BANK[normCategory]?.[normDifficulty] || INTERVIEW_QUESTION_BANK.TECHNICAL.MEDIUM;
  const matchedFallback = fallbackList.find(q => q.skill.toLowerCase() === targetSkill.toLowerCase()) || fallbackList[0];

  const fallbackQuestion = {
    question: matchedFallback.question,
    category: normCategory,
    difficulty: normDifficulty,
    target_skill: targetSkill,
    preparation_tips: matchedFallback.tips,
    isAiGenerated: false
  };

  const apiKey = process.env.GEMINI_API_KEY || config.geminiApiKey;
  if (!apiKey || apiKey === 'mock_key' || apiKey === 'your_gemini_api_key_here' || apiKey.trim() === '') {
    return fallbackQuestion;
  }

  try {
    const ai = new GoogleGenAI({ apiKey });
    const promptText = `You are a Lead Software Engineering Interviewer conducting mock interviews for university students.
Generate ONE interview question tailored to the student's target role and skill gap.

SPECIFICATIONS:
- Target Role: ${targetRole}
- Category: ${normCategory} (must be strictly TECHNICAL, BEHAVIORAL, PROJECT, or ROLE_SPECIFIC)
- Difficulty: ${normDifficulty} (must be strictly EASY, MEDIUM, or HARD)
- Target Skill/Focus: ${targetSkill}

STRICT ANTI-HALLUCINATION RULES:
- Do NOT assume personal details about the student's real-life jobs or identity.
- Frame the question professionally around computer science concepts, problem-solving, or engineering scenarios.

Return JSON in this exact format:
{
  "question": "The question text here...",
  "category": "${normCategory}",
  "difficulty": "${normDifficulty}",
  "target_skill": "${targetSkill}",
  "preparation_tips": "Guidance on how the student should structure their response."
}`;

    const modelsToTry = ['gemini-2.5-flash', 'gemini-2.0-flash', 'gemini-1.5-flash'];
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
            if (parsed.question && typeof parsed.question === 'string' && parsed.question.trim().length > 10) {
              return {
                question: parsed.question.trim(),
                category: normCategory,
                difficulty: normDifficulty,
                target_skill: targetSkill,
                preparation_tips: parsed.preparation_tips || fallbackQuestion.preparation_tips,
                isAiGenerated: true
              };
            }
          }
        }
      } catch (err) {
        // try next
      }
    }
  } catch (err) {
    console.error('[Gemini Interview Question Gen Error]:', err.message || err);
  }

  return fallbackQuestion;
}

/**
 * Deterministically evaluates interview dimensions and percentage.
 * Total points = 20 (4 dimensions * 5 points max each)
 * Percentage = (total / 20) * 100
 */
export function calculateInterviewScore({ relevance = 3, clarity = 3, completeness = 3, technical_understanding = 3 }) {
  // Clamp each dimension strictly to 0 - 5
  const clampedRelevance = Math.max(0, Math.min(5, Number(relevance) || 0));
  const clampedClarity = Math.max(0, Math.min(5, Number(clarity) || 0));
  const clampedCompleteness = Math.max(0, Math.min(5, Number(completeness) || 0));
  const clampedTechnical = Math.max(0, Math.min(5, Number(technical_understanding) || 0));

  const total = clampedRelevance + clampedClarity + clampedCompleteness + clampedTechnical;
  const maxTotal = 20;
  const percentage = Number(((total / maxTotal) * 100).toFixed(2));

  return {
    relevance: clampedRelevance,
    clarity: clampedClarity,
    completeness: clampedCompleteness,
    technical_understanding: clampedTechnical,
    total,
    max_total: maxTotal,
    percentage,
    score_formula: '(relevance + clarity + completeness + technical_understanding) / 20 * 100'
  };
}

/**
 * Fallback answer evaluation rubric.
 */
function generateFallbackEvaluation({ question, answer, category, difficulty, targetSkill }) {
  const wordCount = answer.trim().split(/\s+/).length;
  let relevance = 3;
  let clarity = 3;
  let completeness = 3;
  let technical = 3;

  if (wordCount > 60) {
    relevance = 4;
    clarity = 4;
    completeness = 4;
    technical = 4;
  } else if (wordCount < 15) {
    relevance = 2;
    clarity = 2;
    completeness = 2;
    technical = 2;
  }

  const scores = calculateInterviewScore({
    relevance,
    clarity,
    completeness,
    technical_understanding: technical
  });

  return {
    scores,
    feedback: {
      strengths: [
        'Attempted to address the interview prompt directly.',
        wordCount > 40 ? 'Provided adequate length and introductory context.' : 'Concise response.'
      ],
      improvements: [
        'Elaborate with specific technical mechanisms or concrete project trade-offs.',
        'Use the STAR method (Situation, Task, Action, Result) for structured storytelling.'
      ],
      overall_feedback: `Your response shows an initial understanding of ${targetSkill || 'the topic'}. To score higher, deepen the explanation of underlying implementation details.`,
      follow_up_tip: `Practice answering with a specific code example or architectural trade-off relating to ${targetSkill || 'this skill'}.`,
      isAiFeedback: false
    }
  };
}

/**
 * Evaluates student answer using Gemini with strict deterministic score calculation.
 */
export async function evaluateInterviewAnswerAI({
  question,
  answer,
  category = 'TECHNICAL',
  difficulty = 'MEDIUM',
  targetSkill = 'Software Engineering',
  targetRole = 'Full Stack Developer'
}) {
  const fallback = generateFallbackEvaluation({ question, answer, category, difficulty, targetSkill });

  const apiKey = process.env.GEMINI_API_KEY || config.geminiApiKey;
  if (!apiKey || apiKey === 'mock_key' || apiKey === 'your_gemini_api_key_here' || apiKey.trim() === '') {
    return fallback;
  }

  try {
    const ai = new GoogleGenAI({ apiKey });
    const promptText = `You are a Technical Interview Evaluator.
Evaluate the student's interview response objectively.

STRICT CONSTRAINTS:
1. Rate each dimension as an integer or decimal between 0.0 and 5.0:
   - relevance (0 to 5): How well does the answer address the specific question?
   - clarity (0 to 5): How clear, structured, and easy to understand is the explanation?
   - completeness (0 to 5): Did the student cover the key aspects or edge cases?
   - technical_understanding (0 to 5): Does the student demonstrate accurate technical concepts?
2. Do NOT speculate about the student's personality, IQ, or mental health.
3. Label all comments as AI feedback.
4. Do NOT invent achievements or external claims.

QUESTION:
"${question}"

TARGET ROLE: ${targetRole}
CATEGORY: ${category}
DIFFICULTY: ${difficulty}
TARGET SKILL: ${targetSkill}

STUDENT'S ANSWER:
"${answer}"

Return JSON strictly in this format:
{
  "relevance": 4.0,
  "clarity": 4.0,
  "completeness": 3.5,
  "technical_understanding": 4.0,
  "strengths": ["point 1", "point 2"],
  "improvements": ["point 1", "point 2"],
  "overall_feedback": "Summary paragraph of feedback...",
  "follow_up_tip": "One actionable tip to improve in this interview category..."
}`;

    const modelsToTry = ['gemini-2.5-flash', 'gemini-2.0-flash', 'gemini-1.5-flash'];
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
            // Deterministically calculate score through backend formula
            const scores = calculateInterviewScore({
              relevance: parsed.relevance,
              clarity: parsed.clarity,
              completeness: parsed.completeness,
              technical_understanding: parsed.technical_understanding
            });

            return {
              scores,
              feedback: {
                strengths: Array.isArray(parsed.strengths) ? parsed.strengths : fallback.feedback.strengths,
                improvements: Array.isArray(parsed.improvements) ? parsed.improvements : fallback.feedback.improvements,
                overall_feedback: parsed.overall_feedback || fallback.feedback.overall_feedback,
                follow_up_tip: parsed.follow_up_tip || fallback.feedback.follow_up_tip,
                isAiFeedback: true
              }
            };
          }
        }
      } catch (err) {
        // try next
      }
    }
  } catch (err) {
    console.error('[Gemini Interview Evaluation Error]:', err.message || err);
  }

  return fallback;
}

/**
 * Aggregates practice history to deterministically identify weak interview categories & skills.
 */
export function analyzeInterviewWeakAreas(practices = []) {
  if (!Array.isArray(practices) || practices.length === 0) {
    return {
      total_attempts: 0,
      average_score: 0,
      category_breakdown: {},
      skill_breakdown: {},
      weak_areas: [],
      strong_areas: []
    };
  }

  const categoryMap = {};
  const skillMap = {};
  let totalScoreSum = 0;

  practices.forEach(p => {
    const cat = p.category || 'TECHNICAL';
    const skill = p.target_skill || 'General';
    const percentage = Number(p.scores?.percentage || 0);

    totalScoreSum += percentage;

    // Category breakdown
    if (!categoryMap[cat]) {
      categoryMap[cat] = { attempts: 0, total_percentage: 0 };
    }
    categoryMap[cat].attempts += 1;
    categoryMap[cat].total_percentage += percentage;

    // Skill breakdown
    if (!skillMap[skill]) {
      skillMap[skill] = { attempts: 0, total_percentage: 0 };
    }
    skillMap[skill].attempts += 1;
    skillMap[skill].total_percentage += percentage;
  });

  const category_breakdown = {};
  const weak_areas = [];
  const strong_areas = [];

  Object.entries(categoryMap).forEach(([cat, data]) => {
    const avg = Number((data.total_percentage / data.attempts).toFixed(2));
    category_breakdown[cat] = {
      attempts: data.attempts,
      average_percentage: avg
    };

    if (avg < 70) {
      weak_areas.push({
        category: cat,
        average_percentage: avg,
        status: 'NEEDS_IMPROVEMENT',
        note: `Average performance is below target 70%. Focus on additional ${cat.toLowerCase()} practice.`
      });
    } else {
      strong_areas.push({
        category: cat,
        average_percentage: avg,
        status: 'PROFICIENT',
        note: `Solid demonstration of ${cat.toLowerCase()} competencies.`
      });
    }
  });

  const skill_breakdown = {};
  Object.entries(skillMap).forEach(([skill, data]) => {
    skill_breakdown[skill] = {
      attempts: data.attempts,
      average_percentage: Number((data.total_percentage / data.attempts).toFixed(2))
    };
  });

  // Sort weak areas from lowest to highest score
  weak_areas.sort((a, b) => a.average_percentage - b.average_percentage);

  const average_score = Number((totalScoreSum / practices.length).toFixed(2));

  return {
    total_attempts: practices.length,
    average_score,
    category_breakdown,
    skill_breakdown,
    weak_areas,
    strong_areas
  };
}
