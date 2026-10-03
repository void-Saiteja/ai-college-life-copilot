import { GoogleGenAI } from '@google/genai';
import { fileURLToPath } from 'url';
import { config } from '../src/config/env.js';

function maskKey(key) {
  if (!key || typeof key !== 'string') return 'none';
  if (key.length <= 8) return '***';
  return `${key.slice(0, 4)}...${key.slice(-4)}`;
}

/**
 * Diagnostic tool to verify accessibility of Google Gemini models.
 */
async function verifyGeminiModels(customKey) {
  const apiKey = customKey || process.env.GEMINI_API_KEY || config.gemini?.apiKey || config.geminiApiKey;

  console.log('==================================================');
  console.log('🔍 Google Gemini API Model Accessibility Verification');
  console.log('==================================================');
  console.log(`Using API Key: ${maskKey(apiKey)}`);

  if (!apiKey || apiKey === 'mock_key' || apiKey === 'your_gemini_api_key_here' || apiKey.trim() === '') {
    console.log('\n❌ No valid GEMINI_API_KEY detected in environment or arguments.');
    console.log('Provide via: GEMINI_API_KEY=your_key node scripts/verify_gemini_models.js [optional_key]');
    return { success: false, reason: 'NO_API_KEY' };
  }

  const ai = new GoogleGenAI({ apiKey });
  const results = {};

  // 1. Test gemini-3.5-flash
  console.log('\nTesting 1/3: gemini-3.5-flash (Text Generation)...');
  try {
    const res = await ai.models.generateContent({
      model: 'gemini-3.5-flash',
      contents: 'Respond with exactly the single word: OK'
    });
    const text = (res?.text || '').trim();
    console.log(`  ✅ gemini-3.5-flash is accessible. Output: "${text}"`);
    results['gemini-3.5-flash'] = { accessible: true, status: 200, preview: text };
  } catch (err) {
    console.error(`  ❌ gemini-3.5-flash failed: ${err.message}`);
    results['gemini-3.5-flash'] = { accessible: false, error: err.message };
  }

  // 2. Test gemini-3.5-flash-lite
  console.log('\nTesting 2/3: gemini-3.5-flash-lite (Fallback Generation)...');
  try {
    const res = await ai.models.generateContent({
      model: 'gemini-3.5-flash-lite',
      contents: 'Respond with exactly the single word: OK'
    });
    const text = (res?.text || '').trim();
    console.log(`  ✅ gemini-3.5-flash-lite is accessible. Output: "${text}"`);
    results['gemini-3.5-flash-lite'] = { accessible: true, status: 200, preview: text };
  } catch (err) {
    console.error(`  ❌ gemini-3.5-flash-lite failed: ${err.message}`);
    results['gemini-3.5-flash-lite'] = { accessible: false, error: err.message };
  }

  // 3. Test gemini-embedding-2 with outputDimensionality: 768
  console.log('\nTesting 3/3: gemini-embedding-2 (Semantic Embeddings with 768-dim)...');
  try {
    const res = await ai.models.embedContent({
      model: 'gemini-embedding-2',
      contents: 'Official university attendance requirement is 75 percent.',
      config: {
        outputDimensionality: 768
      }
    });
    const values = res.embeddings?.[0]?.values || res.embedding?.values;
    const isArray = Array.isArray(values);
    const len = isArray ? values.length : 0;
    const allFinite = isArray && values.every(Number.isFinite);

    if (isArray && len === 768 && allFinite) {
      console.log(`  ✅ gemini-embedding-2 is accessible. Vector dim: ${len} (All finite numbers).`);
      results['gemini-embedding-2'] = { accessible: true, dimension: len, valid: true };
    } else {
      console.error(`  ❌ gemini-embedding-2 returned unexpected vector: len=${len}, allFinite=${allFinite}`);
      results['gemini-embedding-2'] = { accessible: false, dimension: len, valid: false };
    }
  } catch (err) {
    console.error(`  ❌ gemini-embedding-2 failed: ${err.message}`);
    results['gemini-embedding-2'] = { accessible: false, error: err.message };
  }

  console.log('\n==================================================');
  console.log('Summary of Model Tests:');
  console.log(JSON.stringify(results, null, 2));
  console.log('==================================================');

  return results;
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  const argKey = process.argv[2];
  verifyGeminiModels(argKey).catch(err => {
    console.error('Fatal execution error:', err.message || err);
    process.exit(1);
  });
}

export { verifyGeminiModels };
