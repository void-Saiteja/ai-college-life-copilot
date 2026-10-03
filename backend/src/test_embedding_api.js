import { GoogleGenAI } from '@google/genai';
import { config } from './config/env.js';

async function testEmbeddingAPI() {
  const apiKey = config.geminiApiKey || process.env.GEMINI_API_KEY;
  console.log('Testing embedding API with key:', apiKey ? (apiKey.substring(0, 8) + '...') : 'None');

  if (!apiKey || apiKey === 'mock_key' || apiKey.trim() === '') {
    console.log('Skipping live API call - no real GEMINI_API_KEY configured.');
    return;
  }

  try {
    const ai = new GoogleGenAI({ apiKey });
    const response = await ai.models.embedContent({
      model: 'text-embedding-004',
      contents: 'Students must maintain a minimum attendance of 75 percent.',
    });

    console.log('\n--- EMBEDDING API RESPONSE ---');
    console.log('Embedding Values Count:', response.embedding?.values?.length);
    console.log('First 5 Values:', response.embedding?.values?.slice(0, 5));
  } catch (err) {
    console.error('Embedding API Error:', err.message || err);
  }
}

testEmbeddingAPI();
