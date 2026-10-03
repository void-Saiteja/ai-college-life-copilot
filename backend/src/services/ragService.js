import { retrieveRelevantChunks } from './retrievalService.js';

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

  const sources = topChunks.map(c => ({
    documentTitle: c.documentTitle,
    pageNumber: c.page_number || 1,
    similarityScore: Math.round(c.score * 100) / 100,
    snippet: c.content
  }));

  const combinedContent = topChunks.map(c => c.content).join('\n\n');

  let answerText = `Based on official document "${sources[0].documentTitle}" (Page ${sources[0].pageNumber}):\n\n${combinedContent}`;

  return {
    found: true,
    answer: answerText,
    sources
  };
}
