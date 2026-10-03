import { GoogleGenAI } from '@google/genai';
import { config } from '../config/env.js';

/**
 * Calculates deterministic attendance metrics and risk classifications.
 */
export function calculateAttendanceMetrics({ conducted = 0, attended = 0, targetPercentage = 75 }) {
  const total = Math.max(0, Number(conducted || 0));
  const att = Math.max(0, Number(attended || 0));
  const target = Math.max(1, Math.min(100, Number(targetPercentage || 75)));

  if (total === 0) {
    return {
      conducted: 0,
      attended: 0,
      percentage: 0.00,
      targetPercentage: target,
      maxCanMiss: 0,
      minRequiredToAttend: 0,
      status: 'No Classes Conducted',
      riskClassification: 'NO_CLASSES'
    };
  }

  const percentage = Number(((att / total) * 100).toFixed(2));
  const targetDecimal = target / 100;

  let maxCanMiss = 0;
  let minRequiredToAttend = 0;

  if (percentage >= target) {
    // If student is at or above target, calculate how many consecutive classes they can miss
    maxCanMiss = Math.max(0, Math.floor((att / targetDecimal) - total));
    minRequiredToAttend = 0;
  } else {
    // If student is below target, calculate how many consecutive classes they must attend
    maxCanMiss = 0;
    const required = ((targetDecimal * total) - att) / (1 - targetDecimal);
    minRequiredToAttend = Math.max(0, Math.ceil(required));
  }

  let riskClassification = 'SAFE';
  let status = `Eligible (Above ${target}%)`;

  if (percentage < target) {
    riskClassification = 'CRITICAL';
    status = `High Risk (<${target}%)`;
  } else if (percentage < target + 5) {
    riskClassification = 'WARNING';
    status = `At Risk / Warning (${target}-${target + 5}%)`;
  } else {
    riskClassification = 'SAFE';
    status = `Safe / Good (>=${target + 5}%)`;
  }

  return {
    conducted: total,
    attended: att,
    percentage,
    targetPercentage: target,
    maxCanMiss,
    minRequiredToAttend,
    status,
    riskClassification
  };
}

/**
 * Calculates future attendance projections based on additional attended/missed classes.
 */
export function calculateProjectionMetrics({ conducted = 0, attended = 0, futureAttended = 0, futureMissed = 0, targetPercentage = 75 }) {
  const current = calculateAttendanceMetrics({ conducted, attended, targetPercentage });

  const futAtt = Math.max(0, Number(futureAttended || 0));
  const futMiss = Math.max(0, Number(futureMissed || 0));

  const projectedConducted = current.conducted + futAtt + futMiss;
  const projectedAttended = current.attended + futAtt;

  const projected = calculateAttendanceMetrics({
    conducted: projectedConducted,
    attended: projectedAttended,
    targetPercentage
  });

  return {
    current,
    futureInput: {
      futureAttended: futAtt,
      futureMissed: futMiss
    },
    projected: {
      conducted: projected.conducted,
      attended: projected.attended,
      percentage: projected.percentage,
      status: projected.status,
      riskClassification: projected.riskClassification,
      maxCanMiss: projected.maxCanMiss,
      minRequiredToAttend: projected.minRequiredToAttend
    },
    disclaimer: 'Note: These calculations are mathematical projections for academic planning.'
  };
}

/**
 * Generates AI attendance breakdown & recommendations via Gemini using calculated values.
 */
export async function generateAttendanceExplanation(metrics) {
  const apiKey = process.env.GEMINI_API_KEY || config.geminiApiKey;
  
  const {
    subjectCode = 'CS',
    subjectName = 'Subject',
    conducted,
    attended,
    percentage,
    targetPercentage = 75,
    maxCanMiss,
    minRequiredToAttend,
    riskClassification
  } = metrics;

  const promptText = `You are an AI Academic Advisor for college students.
Explain the following student's attendance situation using the exact calculated backend numbers provided.

ATTENDANCE DATA:
- Subject: ${subjectCode} - ${subjectName}
- Classes Conducted: ${conducted}
- Classes Attended: ${attended}
- Current Attendance: ${percentage}%
- Required Attendance Target: ${targetPercentage}%
- Maximum Classes Still Allowed to Miss: ${maxCanMiss}
- Minimum Consecutive Classes Required to Attend: ${minRequiredToAttend}
- Risk Level: ${riskClassification}

Provide a concise (3-4 sentences), encouraging, and clear explanation of their situation and actionable advice on what they must do next.`;

  // Fallback string if Gemini API key missing or call fails
  const fallbackExplanation = generateFallbackExplanation(metrics);

  if (!apiKey || apiKey === 'mock_key' || apiKey === 'your_gemini_api_key_here' || apiKey.trim() === '') {
    return { success: true, explanation: fallbackExplanation, isFallback: true };
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
          return { success: true, explanation: response.text.trim(), isFallback: false };
        }
      } catch (err) {
        // try next model
      }
    }
  } catch (err) {
    console.error('[Gemini Attendance Explanation Error]:', err.message || err);
  }

  return { success: true, explanation: fallbackExplanation, isFallback: true };
}

function generateFallbackExplanation(metrics) {
  const { subjectName = 'Subject', percentage, targetPercentage = 75, maxCanMiss, minRequiredToAttend, riskClassification } = metrics;

  if (riskClassification === 'CRITICAL') {
    return `Your attendance in ${subjectName} is currently at ${percentage}%, which is below the required ${targetPercentage}% threshold. You must attend the next ${minRequiredToAttend} consecutive classes without missing any to restore exam eligibility.`;
  } else if (riskClassification === 'WARNING') {
    return `Your attendance in ${subjectName} is at ${percentage}%, meeting the minimum ${targetPercentage}% target. However, your margin is slim—you can only afford to miss ${maxCanMiss} more class(es) before falling into the critical zone.`;
  } else if (riskClassification === 'NO_CLASSES') {
    return `No classes have been conducted yet for ${subjectName}. Maintain regular attendance once classes start to stay above ${targetPercentage}%.`;
  } else {
    return `Great job! Your attendance in ${subjectName} is at a strong ${percentage}%, well above the ${targetPercentage}% requirement. You have a safety buffer of ${maxCanMiss} class(es) you could miss if needed.`;
  }
}
