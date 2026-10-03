import { GoogleGenAI } from '@google/genai';
import { config } from '../config/env.js';

/**
 * Generates a real numerical semantic embedding vector using Google Gemini's gemini-embedding-2 model.
 * Enforces production safety: In production, missing key or API failures throw explicit errors instead of silently creating fake embeddings.
 * @param {string} text - Input text string
 * @returns {Promise<{ values: number[], provider: string, model: string, dimension: number, version: string }>}
 */
export async function generateSemanticEmbedding(text) {
  const isProd = config.nodeEnv === 'production' || process.env.STRICT_EMBEDDINGS === 'true';
  const targetModel = config.gemini?.embeddingModel || 'gemini-embedding-2';
  const targetDim = config.gemini?.embeddingDimension || 768;
  const targetVersion = config.gemini?.embeddingVersion || 'v1';

  if (!text || typeof text !== 'string' || text.trim() === '') {
    return {
      values: new Array(targetDim).fill(0),
      provider: 'google',
      model: targetModel,
      dimension: targetDim,
      version: targetVersion
    };
  }

  const apiKey = process.env.GEMINI_API_KEY || config.gemini?.apiKey || config.geminiApiKey;
  const hasValidKey = apiKey && apiKey !== 'mock_key' && apiKey !== 'your_gemini_api_key_here' && apiKey.trim() !== '';

  if (hasValidKey) {
    try {
      const ai = new GoogleGenAI({ apiKey });
      const response = await ai.models.embedContent({
        model: targetModel,
        contents: text.trim(),
        config: {
          outputDimensionality: targetDim
        }
      });

      const values = response?.embeddings?.[0]?.values || response?.embedding?.values;

      if (Array.isArray(values)) {
        if (values.length !== targetDim) {
          throw new Error(`Expected embedding dimension ${targetDim}, but received ${values.length}`);
        }
        if (!values.every(Number.isFinite)) {
          throw new Error('Embedding vector contains non-finite numerical values');
        }

        return {
          values,
          provider: 'google',
          model: targetModel,
          dimension: values.length,
          version: targetVersion
        };
      }
      throw new Error('Empty or invalid embedding response received from Google Gemini API');
    } catch (err) {
      console.error('[Embedding API Failure]:', err.message || err);
      
      // In production mode, do NOT silently create fake fallback embeddings
      if (isProd) {
        throw new Error(`Google Semantic Embedding API call failed: ${err.message || 'API Error'}. Production requires live Google embeddings.`);
      }
    }
  }

  // In production mode, missing key is a strict error
  if (isProd) {
    throw new Error('GEMINI_API_KEY is missing. Real Google gemini-embedding-2 API is required in production.');
  }

  // Development-only local fallback with explicit provider metadata notice
  console.warn('[Dev Notice]: Using local fallback feature vector (GEMINI_API_KEY missing/mock in dev environment).');
  const fallbackValues = computeLocalSemanticVector(text);
  
  return {
    values: fallbackValues,
    provider: 'local-fallback',
    model: 'hash-128',
    dimension: fallbackValues.length,
    version: 'local'
  };
}

/**
 * Calculates Cosine Similarity between two embedding objects or float vectors.
 * Verifies vector dimension compatibility (e.g. 768 vs 768).
 * @param {object|number[]} vecA - Vector A or Embedding Object A
 * @param {object|number[]} vecB - Vector B or Embedding Object B
 * @returns {number} Cosine similarity score between -1.0 and 1.0
 */
export function cosineSimilarity(vecA, vecB) {
  const valsA = extractVectorValues(vecA);
  const valsB = extractVectorValues(vecB);

  if (!valsA || !valsB || valsA.length === 0 || valsB.length === 0) {
    return 0;
  }

  // Incompatible vector dimensions (e.g. comparing 768-dim with 128-dim or object maps)
  if (valsA.length !== valsB.length) {
    return 0;
  }

  let dotProduct = 0;
  let normA = 0;
  let normB = 0;

  for (let i = 0; i < valsA.length; i++) {
    const valA = valsA[i] || 0;
    const valB = valsB[i] || 0;
    dotProduct += valA * valB;
    normA += valA * valA;
    normB += valB * valB;
  }

  if (normA === 0 || normB === 0) return 0;

  return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
}

/**
 * Helper to extract float array from vector or embedding metadata object.
 */
function extractVectorValues(vec) {
  if (!vec) return null;
  if (Array.isArray(vec)) return vec;
  if (typeof vec === 'object' && Array.isArray(vec.values)) return vec.values;
  return null;
}

/**
 * Local deterministic semantic feature vector generator (for offline / keyless dev testing).
 */
function computeLocalSemanticVector(text) {
  const DIM = 128;
  const vector = new Array(DIM).fill(0);
  const words = text.toLowerCase().match(/\b\w+\b/g) || [];

  if (words.length === 0) return vector;

  words.forEach(w => {
    let hash = 0;
    for (let i = 0; i < w.length; i++) {
      hash = (hash << 5) - hash + w.charCodeAt(i);
      hash |= 0;
    }
    const idx = Math.abs(hash) % DIM;
    vector[idx] += 1;
  });

  // Normalize to unit vector
  let norm = 0;
  for (let i = 0; i < DIM; i++) norm += vector[i] * vector[i];
  norm = Math.sqrt(norm);

  if (norm > 0) {
    for (let i = 0; i < DIM; i++) vector[i] /= norm;
  }

  return vector;
}
