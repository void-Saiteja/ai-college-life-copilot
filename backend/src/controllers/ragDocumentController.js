import path from 'path';
import { getDB, saveDB } from '../storage/db.js';
import { extractPagesFromPDFBuffer, generateOverlappingChunks } from '../services/pdfService.js';
import { generateSemanticEmbedding } from '../services/embeddingService.js';
import { answerRAGQuery } from '../services/ragService.js';

export const getDocuments = (req, res, next) => {
  try {
    const db = getDB();
    res.json({ success: true, data: db.documents || [] });
  } catch (error) {
    next(error);
  }
};

export const uploadDocument = async (req, res, next) => {
  try {
    const file = req.file;
    const { title } = req.body;

    if (!file && !req.body.text) {
      return res.status(400).json({ success: false, error: 'Document file or text content is required' });
    }

    const db = getDB();
    const docId = 'doc-' + Date.now();
    const rawTitle = title || file?.originalname || 'College Policy Document';
    const docTitle = path.basename(rawTitle).replace(/[^a-zA-Z0-9._ -]/g, '_');
    const fileSize = file?.size || (req.body.text ? req.body.text.length : 1000);

    let extractedPages = [];

    if (file) {
      // Validate PDF file format
      const isPDF = file.mimetype === 'application/pdf' || file.originalname.toLowerCase().endsWith('.pdf') || file.buffer.toString('utf-8', 0, 5) === '%PDF-';
      
      if (!isPDF && !req.body.text) {
        return res.status(400).json({ success: false, error: 'Uploaded file must be a valid PDF document' });
      }

      // 1. Real PDF parsing with page-level extraction
      const pdfData = await extractPagesFromPDFBuffer(file.buffer);
      extractedPages = pdfData.pages;
    } else if (req.body.text) {
      // Direct text input
      extractedPages = [{ pageNumber: 1, text: req.body.text }];
    }

    if (!extractedPages || extractedPages.length === 0) {
      return res.status(400).json({ success: false, error: 'No readable text content extracted from document' });
    }

    // 2. Overlapping chunk generation with actual page metadata
    const newChunks = generateOverlappingChunks(extractedPages, docId, docTitle);

    // 3. Generate semantic embedding for each chunk during ingestion (fails fast if API fails)
    for (const chunk of newChunks) {
      try {
        chunk.embedding = await generateSemanticEmbedding(chunk.content);
      } catch (err) {
        console.error('[Document Ingestion Embedding Error]:', err.message || err);
        return res.status(500).json({
          success: false,
          error: `Document indexing failed: ${err.message || 'Unable to generate semantic embeddings'}`
        });
      }
    }

    // 4. Save document & chunks to database
    const newDoc = {
      id: docId,
      title: docTitle,
      file_name: file?.originalname || `${docTitle}.pdf`,
      file_size: fileSize,
      uploaded_by: req.user?.id || 'u-admin-1',
      page_count: extractedPages.length,
      chunk_count: newChunks.length,
      created_at: new Date().toISOString()
    };

    db.documents.push(newDoc);
    db.document_chunks.push(...newChunks);
    saveDB(db);

    res.status(201).json({
      success: true,
      message: 'Document uploaded, chunked, and semantically indexed successfully',
      data: newDoc
    });

  } catch (error) {
    next(error);
  }
};

export const queryRAG = async (req, res, next) => {
  try {
    const { query } = req.body;
    if (!query) {
      return res.status(400).json({ success: false, error: 'Search query is required' });
    }

    const result = await answerRAGQuery(query);
    res.json({ success: true, data: result });
  } catch (error) {
    next(error);
  }
};

export const deleteDocument = (req, res, next) => {
  try {
    const { id } = req.params;
    const db = getDB();

    db.documents = (db.documents || []).filter(d => d.id !== id);
    db.document_chunks = (db.document_chunks || []).filter(c => c.document_id !== id);
    saveDB(db);

    res.json({ success: true, message: 'Document deleted successfully' });
  } catch (error) {
    next(error);
  }
};
