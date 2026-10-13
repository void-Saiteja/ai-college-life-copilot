import { generateSemanticEmbedding, cosineSimilarity } from './embeddingService.js';
import { getDB } from '../storage/db.js';
import { getPool, isMySQL } from '../config/db.js';
import { config } from '../config/env.js';

const STOP_WORDS = new Set([
  'what', 'when', 'where', 'which', 'who', 'whom', 'whose', 'why', 'how',
  'does', 'do', 'did', 'have', 'has', 'had', 'this', 'that', 'these', 'those',
  'about', 'with', 'from', 'into', 'during', 'before', 'after', 'above', 'below',
  'more', 'most', 'other', 'some', 'such', 'only', 'same', 'than', 'too', 'very',
  'can', 'will', 'just', 'should', 'now', 'the', 'a', 'an', 'is', 'are', 'was', 'were'
]);

/**
 * Computes normalized lexical token overlap between query and document text.
 */
function computeLexicalOverlap(query, content) {
  if (!query || !content) return 0.0;
  const qTokens = query.toLowerCase().split(/\W+/);
  const terms = qTokens.filter(t => t.length > 2 && !STOP_WORDS.has(t));
  if (terms.length === 0) return 0.0;

  const contentLower = content.toLowerCase();
  let matched = 0;
  for (const t of terms) {
    if (contentLower.includes(t)) matched++;
  }
  if (matched === 0) return 0.0;
  const ratio = matched / terms.length;
  return ratio >= 1.0 ? 0.85 : ratio * 0.70;
}

/**
 * Detects if a query asks about attendance policy, rules, or examination attendance eligibility.
 */
export function isAttendanceQuery(query) {
  if (!query || typeof query !== 'string') return false;
  const q = query.toLowerCase();
  return q.includes('attendance') || q.includes('absent') || q.includes('bunk') ||
         (q.includes('eligible') && (q.includes('exam') || q.includes('semester'))) ||
         (q.includes('minimum') && (q.includes('class') || q.includes('subject')));
}

/**
 * Detects if a query specifically asks about admission cancellation, fees, or refunds.
 */
export function isCancellationQuery(query) {
  if (!query || typeof query !== 'string') return false;
  return /\b(cancel|cancellation|refund|fee|fees|tuition|deposit|seat withdrawal)\b/i.test(query);
}

/**
 * Filters chunks to directly supporting sources, strictly excluding unrelated topics.
 * For attendance questions:
 * - Prioritizes chunks detailing attendance policy requirements.
 * - Strictly excludes admission cancellation, refund, fee policies, or unrelated grading formulas.
 */
export function filterSupportingChunks(query, chunks) {
  if (!query || !chunks || chunks.length === 0) return [];
  const qLower = query.toLowerCase();
  const attendanceIntent = isAttendanceQuery(query);
  const cancellationIntent = isCancellationQuery(query);

  return chunks.filter(chunk => {
    const titleLower = (chunk.documentTitle || chunk.document_title || '').toLowerCase();
    const contentLower = (chunk.content || '').toLowerCase();

    // If query is about attendance requirements and NOT explicitly asking about cancellation/refund:
    if (attendanceIntent && !cancellationIntent) {
      // Exclude chunks whose title or content is primarily about admission cancellation or fee refunds
      const isCancellationOrRefund = titleLower.includes('cancellation') || titleLower.includes('refund') ||
                                    contentLower.includes('cancellation policy') || contentLower.includes('fee refund') ||
                                    contentLower.includes('seat cancellation') || contentLower.includes('admission withdrawal');
      if (isCancellationOrRefund) {
        return false;
      }

      // Exclude chunks that are solely about grading system/CIE percentages if query did not ask about grades/marks
      const isGradingOnly = (titleLower.includes('grading') || contentLower.includes('grading system')) &&
                            !contentLower.includes('attendance');
      if (isGradingOnly && !qLower.includes('grade') && !qLower.includes('grading') && !qLower.includes('gpa')) {
        return false;
      }

      // Exclude library or facility rules if query did not ask about library or lab access
      const isLibraryOnly = (titleLower.includes('library') || contentLower.includes('library & lab rules')) &&
                            !contentLower.includes('attendance');
      if (isLibraryOnly && !qLower.includes('library') && !qLower.includes('lab')) {
        return false;
      }

      // Exclude scholarship or financial aid rules if query did not ask about scholarships/aid
      const isScholarshipOnly = (titleLower.includes('scholarship') || contentLower.includes('scholarship') || contentLower.includes('financial aid')) &&
                                !contentLower.includes('attendance');
      if (isScholarshipOnly && !qLower.includes('scholarship') && !qLower.includes('aid')) {
        return false;
      }
    }

    return true;
  });
}

/**
 * Deduplicates retrieved chunks by document title and page number.
 */
export function deduplicateSources(chunks = []) {
  if (!chunks || !Array.isArray(chunks)) return [];
  const seen = new Set();
  const deduped = [];
  for (const chunk of chunks) {
    const docName = chunk.documentTitle || chunk.document_title || chunk.documentName || 'Official Document';
    const pageNum = chunk.page_number || chunk.pageNumber || 1;
    const key = `${docName.trim().toLowerCase()}:::${pageNum}`;
    if (!seen.has(key)) {
      seen.add(key);
      deduped.push(chunk);
    }
  }
  return deduped;
}

/**
 * Retrieves the top relevant document chunks for a query using semantic vector embeddings
 * with hybrid lexical ranking and domain-relevance filtering.
 * Strict production safety: NEVER dynamically modifies or re-embeds document chunks at query time.
 * Chunks with missing or incompatible embedding models/dimensions are safely skipped with a warning.
 * @param {string} query - User search query
 * @param {number} topK - Maximum number of chunks to return (default 3)
 * @param {number} threshold - Relevance threshold (default process.env.RAG_SIMILARITY_THRESHOLD || 0.35)
 * @returns {Promise<Array<{ id: string, documentTitle: string, page_number: number, chunk_index: number, content: string, score: number }>>}
 */
export async function retrieveRelevantChunks(query, topK = 3, threshold = parseFloat(process.env.RAG_SIMILARITY_THRESHOLD || '0.35')) {
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

    const vectorScore = cosineSimilarity(queryEmbObj, vec);
    const lexicalScore = computeLexicalOverlap(query, chunk.content);
    // Combined hybrid score takes the strongest signal
    const score = Math.max(vectorScore, lexicalScore);

    const doc = documents.find(d => d.id === chunk.document_id);
    const docTitle = chunk.document_title || chunk.document_name || doc?.title || doc?.file_name || 'College Academic Rules & Attendance Policy 2026.pdf';

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

  // 1. Sort chunks in descending order of relevance score
  scored.sort((a, b) => b.score - a.score);

  // 2. Filter out unrelated chunks based on query intent (e.g. cancellation policy for attendance queries)
  const topicFiltered = filterSupportingChunks(query, scored);

  // 3. Filter chunks meeting minimum relevance threshold
  const thresholdFiltered = topicFiltered.filter(c => c.score >= threshold);

  // 4. Do not include trailing chunks with low relative relevance merely because vector search returned them
  let finalChunks = thresholdFiltered;
  if (thresholdFiltered.length > 1) {
    const topScore = thresholdFiltered[0].score;
    const cutoff = Math.max(threshold, topScore * 0.60);
    finalChunks = thresholdFiltered.filter(c => c.score >= cutoff);
  }

  // 5. Deduplicate sources by document title and page number
  const uniqueChunks = deduplicateSources(finalChunks);
  return uniqueChunks.slice(0, topK);
}
