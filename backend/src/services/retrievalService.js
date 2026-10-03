import { generateSemanticEmbedding, cosineSimilarity } from './embeddingService.js';
import { getDB } from '../storage/db.js';

/**
 * Retrieves the top relevant document chunks for a query using semantic vector embeddings.
 * @param {string} query - User search query
 * @param {number} topK - Maximum number of chunks to return (default 3)
 * @param {number} threshold - Cosine similarity threshold (default process.env.RAG_SIMILARITY_THRESHOLD || 0.10)
 * @returns {Promise<Array<{ id: string, documentTitle: string, page_number: number, chunk_index: number, content: string, score: number }>>}
 */
export async function retrieveRelevantChunks(query, topK = 3, threshold = parseFloat(process.env.RAG_SIMILARITY_THRESHOLD || '0.10')) {
  const db = getDB();
  const chunks = db.document_chunks || [];

  if (!chunks || chunks.length === 0) return [];

  let queryEmbObj;
  try {
    queryEmbObj = await generateSemanticEmbedding(query);
  } catch (err) {
    console.error('[RAG Query Embedding Error]:', err.message || err);
    return [];
  }

  const scored = [];

  for (const chunk of chunks) {
    let chunkEmbObj = chunk.embedding;

    // If chunk missing stored embedding, compute and store it
    if (!chunkEmbObj) {
      try {
        chunkEmbObj = await generateSemanticEmbedding(chunk.content);
        chunk.embedding = chunkEmbObj;
      } catch (err) {
        continue;
      }
    }

    const score = cosineSimilarity(queryEmbObj, chunkEmbObj);
    const doc = (db.documents || []).find(d => d.id === chunk.document_id);

    scored.push({
      id: chunk.id,
      document_id: chunk.document_id,
      documentTitle: chunk.document_name || doc?.title || doc?.file_name || 'College Policy Document',
      page_number: chunk.page_number || 1,
      chunk_index: chunk.chunk_index || 0,
      content: chunk.content,
      score
    });
  }

  // Sort chunks in descending order of semantic similarity
  scored.sort((a, b) => b.score - a.score);

  // Filter chunks meeting minimum semantic similarity threshold
  const relevantChunks = scored.filter(c => c.score >= threshold);

  return relevantChunks.slice(0, topK);
}
