package com.collegecopilot.rag;

import com.collegecopilot.rag.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final PdfExtractionService pdfExtractionService;
    private final GeminiEmbeddingService embeddingService;
    private final GeminiRagService geminiRagService;
    private final RagRetrievalService ragRetrievalService;
    private final ObjectMapper objectMapper;

    @Autowired
    public RagService(DocumentRepository documentRepository,
                      DocumentChunkRepository documentChunkRepository,
                      PdfExtractionService pdfExtractionService,
                      GeminiEmbeddingService embeddingService,
                      GeminiRagService geminiRagService,
                      RagRetrievalService ragRetrievalService) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.pdfExtractionService = pdfExtractionService;
        this.embeddingService = embeddingService;
        this.geminiRagService = geminiRagService;
        this.ragRetrievalService = ragRetrievalService;
        this.objectMapper = JsonMapper.builder().build();
    }

    private void requireAdmin(UserPrincipal principal) {
        if (principal == null || !"ADMIN".equalsIgnoreCase(principal.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator role required");
        }
    }

    @Transactional(readOnly = true)
    public DocumentListResponse getDocuments() {
        List<Document> docs = documentRepository.findAllByOrderByCreatedAtDesc();
        List<DocumentDto> dtos = docs.stream().map(this::mapToDto).collect(Collectors.toList());
        return new DocumentListResponse(true, dtos);
    }

    @Transactional
    public DocumentUploadResponse uploadDocument(MultipartFile file, String title, String text, UserPrincipal principal) {
        requireAdmin(principal);

        boolean hasFile = file != null && !file.isEmpty();
        boolean hasText = text != null && !text.trim().isEmpty();

        if (!hasFile && !hasText) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document file or text content is required");
        }

        String docId = "doc-" + System.currentTimeMillis();
        String rawTitle = (title != null && !title.isBlank()) ? title : (hasFile ? file.getOriginalFilename() : "College Policy Document");
        String docTitle = sanitizeTitle(rawTitle);
        String fileName = hasFile ? file.getOriginalFilename() : docTitle + ".txt";
        int fileSize = hasFile ? (int) file.getSize() : text.length();

        List<ExtractedPage> extractedPages = new ArrayList<>();

        if (hasFile) {
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            String contentType = file.getContentType() != null ? file.getContentType() : "";
            byte[] bytes;
            try {
                bytes = file.getBytes();
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded file buffer");
            }

            boolean isPdf = contentType.equalsIgnoreCase("application/pdf")
                    || originalName.endsWith(".pdf")
                    || (bytes.length >= 4 && new String(bytes, 0, 4).equals("%PDF"));

            if (!isPdf && !hasText) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must be a valid PDF document");
            }

            if (isPdf) {
                try {
                    extractedPages = pdfExtractionService.extractPagesFromPDF(bytes);
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to extract text from PDF: " + e.getMessage());
                }
            } else if (hasText) {
                extractedPages.add(new ExtractedPage(1, text.trim()));
            }
        } else if (hasText) {
            extractedPages.add(new ExtractedPage(1, text.trim()));
        }

        if (extractedPages.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No readable text content extracted from document");
        }

        // Generate overlapping chunks with page metadata
        List<ChunkDto> newChunks = pdfExtractionService.generateOverlappingChunks(extractedPages, docId, docTitle);

        // Generate semantic embeddings for each chunk
        List<DocumentChunk> chunkEntities = new ArrayList<>();
        for (ChunkDto chunk : newChunks) {
            GeminiEmbeddingService.EmbeddingResult emb = embeddingService.generateSemanticEmbedding(chunk.getContent());
            String vectorJson;
            try {
                vectorJson = objectMapper.writeValueAsString(emb.values());
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize vector embedding");
            }

            DocumentChunk chunkEntity = new DocumentChunk(
                    chunk.getId(),
                    docId,
                    chunk.getChunkIndex(),
                    chunk.getContent(),
                    chunk.getPageNumber(),
                    vectorJson,
                    emb.model(),
                    emb.dimension(),
                    emb.version()
            );
            chunkEntities.add(chunkEntity);
        }

        // Save document entity
        Document newDoc = new Document(
                docId,
                docTitle,
                fileName,
                fileSize,
                principal.getId(),
                newChunks.size()
        );
        documentRepository.save(newDoc);
        documentChunkRepository.saveAll(chunkEntities);

        return new DocumentUploadResponse(true, "Document uploaded, chunked, and semantically indexed successfully", mapToDto(newDoc));
    }

    @Transactional
    public DocumentDeleteResponse deleteDocument(String id, UserPrincipal principal) {
        requireAdmin(principal);

        documentChunkRepository.deleteByDocumentId(id);
        documentRepository.deleteById(id);

        return new DocumentDeleteResponse(true, "Document deleted successfully");
    }

    public RAGQueryResponse answerRAGQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query is required");
        }

        double threshold = 0.55;
        List<ChunkDto> topChunks = ragRetrievalService.retrieveRelevantChunks(query.trim(), 3, threshold);

        if (topChunks.isEmpty()) {
            return new RAGQueryResponse(true, new RAGResultDto(
                    false,
                    "I could not find an answer to your query in the uploaded official college documents. Please consult the academic administration office for official guidance.",
                    Collections.emptyList()
            ));
        }

        GeminiRagService.GroundingVerificationResult verification = geminiRagService.answerAndVerifyDocumentQuery(query.trim(), topChunks);

        if (verification != null && "FOUND".equalsIgnoreCase(verification.status()) && verification.answer() != null && !verification.answer().isBlank()) {
            List<RAGSourceDto> sources = topChunks.stream().map(c -> new RAGSourceDto(
                    c.getDocumentTitle(),
                    c.getPageNumber(),
                    Math.round(c.getScore() * 100.0) / 100.0,
                    c.getContent()
            )).collect(Collectors.toList());

            return new RAGQueryResponse(true, new RAGResultDto(true, verification.answer(), sources));
        }

        if (verification != null && verification.isServiceError()) {
            return new RAGQueryResponse(true, new RAGResultDto(
                    false,
                    "I could not verify an answer from the uploaded official college documents right now. Please try again later.",
                    Collections.emptyList()
            ));
        }

        return new RAGQueryResponse(true, new RAGResultDto(
                false,
                "I could not find an answer to your query in the uploaded official college documents. Please consult the academic administration office for official guidance.",
                Collections.emptyList()
        ));
    }

    private DocumentDto mapToDto(Document d) {
        String created = d.getCreatedAt() != null
                ? d.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toString()
                : Instant.now().toString();

        return new DocumentDto(
                d.getId(),
                d.getTitle(),
                d.getFileName(),
                d.getFileSize(),
                d.getUploadedBy(),
                d.getChunkCount(),
                created
        );
    }

    private String sanitizeTitle(String rawTitle) {
        if (rawTitle == null || rawTitle.isBlank()) return "College Policy Document";
        String baseName = new File(rawTitle).getName();
        return baseName.replaceAll("[^a-zA-Z0-9._ -]", "_");
    }
}
