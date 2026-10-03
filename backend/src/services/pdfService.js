import { createRequire } from 'module';
const require = createRequire(import.meta.url);
const pdfModule = require('pdf-parse');
const PDFParse = pdfModule.PDFParse || pdfModule.default || pdfModule;

/**
 * Extracts page-by-page text from a PDF buffer while retaining page numbers.
 * @param {Buffer} buffer - Binary PDF file buffer
 * @returns {Promise<{ numpages: number, pages: Array<{ pageNumber: number, text: string }> }>}
 */
export async function extractPagesFromPDFBuffer(buffer) {
  if (!buffer || !Buffer.isBuffer(buffer)) {
    throw new Error('Invalid PDF file buffer provided');
  }

  try {
    const parser = new PDFParse({ data: buffer });
    const textResult = await parser.getText();

    const pages = [];
    
    // textResult.pages contains [{ text: '...', num: 1 }, { text: '...', num: 2 }, ...]
    if (textResult && Array.isArray(textResult.pages)) {
      for (const p of textResult.pages) {
        const cleanText = cleanTextContent(p.text || '');
        if (cleanText.length > 0) {
          pages.push({
            pageNumber: p.num || (pages.length + 1),
            text: cleanText
          });
        }
      }
    } else if (textResult && textResult.text) {
      // Fallback single page
      pages.push({
        pageNumber: 1,
        text: cleanTextContent(textResult.text)
      });
    }

    pages.sort((a, b) => a.pageNumber - b.pageNumber);

    return {
      numpages: textResult?.total || pages.length,
      pages
    };
  } catch (error) {
    console.error('[PDF Extraction Error]:', error.message || error);
    throw new Error('Failed to parse PDF document: ' + (error.message || 'Corrupted file'));
  }
}

/**
 * Cleans raw extracted PDF text while preserving structure and headings.
 * @param {string} text - Raw extracted text
 * @returns {string} Cleaned text
 */
export function cleanTextContent(text) {
  if (!text) return '';
  return text
    .replace(/\r\n/g, '\n')
    .replace(/[ \t]+/g, ' ')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

/**
 * Creates overlapping text chunks (~500-800 chars, ~100-150 chars overlap) with page numbers.
 * @param {Array<{ pageNumber: number, text: string }>} pages - Array of extracted pages
 * @param {string} docId - Document ID
 * @param {string} docName - Document Title / File Name
 * @param {number} targetChunkSize - Target characters per chunk (default 650)
 * @param {number} overlapSize - Target overlap characters (default 120)
 * @returns {Array<{ id: string, document_id: string, document_name: string, page_number: number, chunk_index: number, content: string }>}
 */
export function generateOverlappingChunks(pages, docId, docName, targetChunkSize = 650, overlapSize = 120) {
  const chunks = [];
  let globalChunkIndex = 0;

  for (const pageObj of pages) {
    const { pageNumber, text } = pageObj;

    if (!text || text.trim().length === 0) continue;

    // Split page text into sentences/paragraphs
    const sentences = text.split(/(?<=[.?!])\s+|\n+/).filter(s => s.trim().length > 0);

    let currentChunk = '';

    for (let i = 0; i < sentences.length; i++) {
      const sentence = sentences[i].trim();

      if (currentChunk.length === 0) {
        currentChunk = sentence;
      } else if (currentChunk.length + sentence.length + 1 <= targetChunkSize) {
        currentChunk += ' ' + sentence;
      } else {
        // Push completed chunk with page number metadata
        chunks.push({
          id: `chk-${Date.now()}-${globalChunkIndex}`,
          document_id: docId,
          document_name: docName,
          page_number: pageNumber,
          chunk_index: globalChunkIndex,
          content: currentChunk.trim()
        });
        globalChunkIndex++;

        // Calculate overlap from tail of currentChunk
        const words = currentChunk.split(' ');
        let overlapText = '';
        for (let j = words.length - 1; j >= 0; j--) {
          if ((words[j] + ' ' + overlapText).length <= overlapSize) {
            overlapText = words[j] + ' ' + overlapText;
          } else {
            break;
          }
        }

        currentChunk = (overlapText.trim() + ' ' + sentence).trim();
      }
    }

    // Push final remaining chunk for the page
    if (currentChunk.trim().length > 0) {
      chunks.push({
        id: `chk-${Date.now()}-${globalChunkIndex}`,
        document_id: docId,
        document_name: docName,
        page_number: pageNumber,
        chunk_index: globalChunkIndex,
        content: currentChunk.trim()
      });
      globalChunkIndex++;
    }
  }

  return chunks;
}
