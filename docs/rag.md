# Retrieval-Augmented Generation (RAG) Architecture

## Overview
College document RAG allows administrators to upload PDF policy documents, which students can then search using natural language queries.

## Pipeline Architecture

```
[ Admin PDF Upload ] 
         |
         v
[ pdfService: Text Extraction ]
         |
         v
[ Text Chunking: ~250 char windows ]
         |
         v
[ embeddingService: Compute Vector Frequency Maps ]
         |
         v
[ Store Chunks in DB / Vector Memory ]
         |
  (Student Query)
         |
         v
[ retrievalService: Cosine Similarity Matching ]
         |
         v
[ Top-3 Relevant Chunks Selected ]
         |
         v
[ ragService: Synthesize Answer + Citation ]
```

## Citation & Source Attribution
- Every answer returns source document title and page number citations (e.g. `Document: Academic_Policy_2026.pdf (Page 1)`).
- If the question cannot be answered from uploaded documents, the AI explicitly states: *"I could not find an answer to your query in the uploaded official college documents."*
