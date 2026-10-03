import { generateSemanticEmbedding, cosineSimilarity } from './embeddingService.js';
import { getDB } from '../storage/db.js';
import { getPool, isMySQL } from '../config/db.js';
import { config } from '../config/env.js';

/**
 * Retrieves the top relevant document chunks for a query using semantic vector embeddings.
 * Strict production safety: NEVER dynamically modifies or re-embeds document chunks at query time.
 * Chunks with missing or incompatible embedding models/dimensions are safely skipped with a warning.
 * @param {string} query - User search query
 * @param {number} topK - Maximum number of chunks to return (default 3)
 * @param {number} threshold - Cosine similarity threshold (default process.env.RAG_SIMILARITY_THRESHOLD || 0.10)
 * @returns {Promise<Array<{ id: string, documentTitle: string, page_number: number, chunk_index: number, content: string, score: number }>>}
 */
export async function retrieveRelevantChunks(query, topK = 3, threshold = parseFloat(process.env.RAG_SIMILARITY_THRESHOLD || '0.10')) {
  let chunks = [];
  let documents = [];

  if (isMySQL()) {
    try {
      const pool = getPool();
      const [chunkRows] = await pool.query(`
        SELECT c.id, c.document_id, c.chunk_index, c.content, c.page_number, 
               c.vector_embedding, c.embedding_model, c.embedding_dimension, c.embedding_version,
               d.title as document_title, d.file_name
        FROM document_chunks c
        LEFT JOIN documents d ON c.document_id = d.id
      `);
      chunks = chunkRows || [];
    } catch (dbErr) {
      console.error('[RAG MySQL Query Warning]:', dbErr.message || dbErr);
      const db = getDB();
      chunks = db.document_chunks || [];
      documents = db.documents || [];
    }
  } else {
    const db = getDB();
    chunks = db.document_chunks || [];
    documents = db.documents || [];
  }

  if (!chunks || chunks.length === 0) return [];

  let queryEmbObj;
  try {
    queryEmbObj = await generateSemanticEmbedding(query);
  } catch (err) {
    console.error('[RAG Query Embedding Error]:', err.message || err);
    return [];
  }

  const expectedModel = config.gemini?.embeddingModel || 'gemini-embedding-2';
  const expectedDim = config.gemini?.embeddingDimension || 768;
  const expectedVersion = config.gemini?.embeddingVersion || 'v1';

  const scored = [];
  let skippedCount = 0;

  for (const chunk of chunks) {
    let vec = chunk.vector_embedding || chunk.embedding;
    let model = chunk.embedding_model;
    let dim = chunk.embedding_dimension;
    let ver = chunk.embedding_version;

    // Handle parsed or unparsed embedding object structures
    if (typeof vec === 'object' && vec !== null && !Array.isArray(vec)) {
      if (vec.values) {
        model = model || vec.model;
        dim = dim || vec.dimension;
        ver = ver || vec.version;
        vec = vec.values;
      }
    } else if (typeof vec === 'string') {
      try {
        const parsed = JSON.parse(vec);
        if (Array.isArray(parsed)) {
          vec = parsed;
        } else if (parsed && parsed.values) {
          model = model || parsed.model;
          dim = dim || parsed.dimension;
          ver = ver || parsed.version;
          vec = parsed.values;
        }
      } catch (e) {
        vec = null;
      }
    }

    // Strict validation: Reject missing, non-array, wrong dimension, or wrong model vectors
    const isModelValid = model === expectedModel;
    const isDimValid = (dim === expectedDim) || (Array.isArray(vec) && vec.length === expectedDim);
    const isVerValid = ver === expectedVersion;
    const isVecArray = Array.isArray(vec) && vec.length === expectedDim;

    if (!isModelValid || !isDimValid || !isVerValid || !isVecArray) {
      skippedCount++;
      console.warn(`[RAG Warning]: Chunk ${chunk.id} skipped due to incompatible embedding metadata (model: ${model || 'null'}, dim: ${dim || (vec ? vec.length : 0)}, ver: ${ver || 'null'}, expected: ${expectedModel}/${expectedDim}/${expectedVersion}).`);
      continue;
    }

    const score = cosineSimilarity(queryEmbObj, vec);
    const doc = documents.find(d => d.id === chunk.document_id);
    const docTitle = chunk.document_title || chunk.document_name || doc?.title || doc?.file_name || 'College Policy Document';

    scored.push({
      id: chunk.id,
      document_id: chunk.document_id,
      documentTitle: docTitle,
      page_number: chunk.page_number || 1,
      chunk_index: chunk.chunk_index || 0,
      content: chunk.content,
      score
    });
  }

  if (skippedCount > 0 && scored.length === 0) {
    console.error(`[RAG Configuration Notice]: All ${skippedCount} candidate chunks skipped due to unmigrated embedding vectors. Re-indexing required.`);
  }

  // Sort chunks in descending order of semantic similarity
  scored.sort((a, b) => b.score - a.score);

  // Filter chunks meeting minimum semantic similarity threshold
  const relevantChunks = scored.filter(c => c.score >= threshold);

  return relevantChunks.slice(0, topK);
}
