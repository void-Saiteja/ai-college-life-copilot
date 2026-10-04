import { retrieveRelevantChunks } from './retrievalService.js';
import { answerAndVerifyDocumentQuery } from './llmService.js';

export async function answerRAGQuery(query) {
  const threshold = parseFloat(process.env.RAG_SIMILARITY_THRESHOLD || '0.55');
  const topChunks = await retrieveRelevantChunks(query, 3, threshold);

  if (!topChunks || topChunks.length === 0) {
    return {
      found: false,
      answer: "I could not find an answer to your query in the uploaded official college documents. Please consult the academic administration office for official guidance.",
      sources: []
    };
  }

  try {
    const verification = await answerAndVerifyDocumentQuery({
      query,
      docChunks: topChunks
    });

    if (verification && verification.status === 'FOUND' && verification.answer) {
      const sources = topChunks.map(c => ({
        documentTitle: c.documentTitle,
        pageNumber: c.page_number || 1,
        similarityScore: Math.round(c.score * 100) / 100,
        snippet: c.content
      }));

      return {
        found: true,
        answer: verification.answer,
        sources
      };
    }

    if (verification && verification.isServiceError) {
      return {
        found: false,
        answer: "I could not verify an answer from the uploaded official college documents right now. Please try again later.",
        sources: []
      };
    }

    return {
      found: false,
      answer: "I could not find an answer to your query in the uploaded official college documents. Please consult the academic administration office for official guidance.",
      sources: []
    };

  } catch (error) {
    console.error('[RAG Grounding Error]: Failed during answer verification:', error.message || error);
    return {
      found: false,
      answer: "I could not verify an answer from the uploaded official college documents right now. Please try again later.",
      sources: []
    };
  }
}
