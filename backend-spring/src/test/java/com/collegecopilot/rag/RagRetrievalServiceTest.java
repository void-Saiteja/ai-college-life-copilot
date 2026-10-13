package com.collegecopilot.rag;

import com.collegecopilot.rag.dto.ChunkDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagRetrievalServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private GeminiEmbeddingService embeddingService;

    private RagRetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        retrievalService = new RagRetrievalService(documentRepository, documentChunkRepository, embeddingService, 0.10);
    }

    @Test
    @DisplayName("retrieveRelevantChunks returns chunks sorted descending by cosine similarity score")
    void retrieveRelevantChunks_sortsDescending() {
        when(embeddingService.getEmbeddingModel()).thenReturn("gemini-embedding-2");
        when(embeddingService.getEmbeddingDimension()).thenReturn(768);
        when(embeddingService.getEmbeddingVersion()).thenReturn("v1");

        double[] queryVec = new double[768];
        queryVec[0] = 1.0;
        when(embeddingService.generateSemanticEmbedding(any())).thenReturn(
                new GeminiEmbeddingService.EmbeddingResult(queryVec, "google", "gemini-embedding-2", 768, "v1")
        );

        StringBuilder dummyVecJson = new StringBuilder("[");
        for (int i = 0; i < 768; i++) {
            dummyVecJson.append("0.1");
            if (i < 767) dummyVecJson.append(",");
        }
        dummyVecJson.append("]");

        DocumentChunk c1 = new DocumentChunk("c-1", "doc-1", 0, "Attendance content", 2, dummyVecJson.toString(), "gemini-embedding-2", 768, "v1");
        DocumentChunk c2 = new DocumentChunk("c-2", "doc-1", 1, "Examination content", 3, dummyVecJson.toString(), "gemini-embedding-2", 768, "v1");

        when(documentChunkRepository.findAll()).thenReturn(List.of(c1, c2));
        when(documentRepository.findAll()).thenReturn(List.of(new Document("doc-1", "Student Handbook", "Handbook.pdf", 1000, "admin", 2)));

        when(embeddingService.cosineSimilarity(any(), any()))
                .thenReturn(0.85) // for c1
                .thenReturn(0.40); // for c2

        List<ChunkDto> results = retrievalService.retrieveRelevantChunks("What is attendance requirement?", 3, 0.50);

        assertEquals(1, results.size());
        assertEquals("c-1", results.get(0).getId());
        assertEquals(0.85, results.get(0).getScore());
        assertEquals(2, results.get(0).getPageNumber());
        assertEquals("Student Handbook", results.get(0).getDocumentTitle());
    }

    @Test
    @DisplayName("retrieveRelevantChunks skips chunks with incompatible dimensions or models")
    void retrieveRelevantChunks_skipsIncompatibleEmbeddings() {
        when(embeddingService.getEmbeddingModel()).thenReturn("gemini-embedding-2");
        when(embeddingService.getEmbeddingDimension()).thenReturn(768);
        when(embeddingService.getEmbeddingVersion()).thenReturn("v1");

        when(embeddingService.generateSemanticEmbedding(any())).thenReturn(
                new GeminiEmbeddingService.EmbeddingResult(new double[768], "google", "gemini-embedding-2", 768, "v1")
        );

        // c1 has wrong dimension 128
        DocumentChunk c1 = new DocumentChunk("c-1", "doc-1", 0, "Old chunk", 1, "[0.1, 0.2]", "gemini-embedding-2", 128, "v1");
        // c2 has wrong model 'legacy-model'
        DocumentChunk c2 = new DocumentChunk("c-2", "doc-1", 1, "Legacy chunk", 1, "[0.1]", "legacy-model", 768, "v1");

        when(documentChunkRepository.findAll()).thenReturn(List.of(c1, c2));
        when(documentRepository.findAll()).thenReturn(Collections.emptyList());

        List<ChunkDto> results = retrievalService.retrieveRelevantChunks("Test query", 3, 0.10);

        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Empty query returns empty chunk list without DB search")
    void retrieveRelevantChunks_emptyQuery_returnsEmpty() {
        List<ChunkDto> results = retrievalService.retrieveRelevantChunks("", 3, 0.10);
        assertTrue(results.isEmpty());
        verify(documentChunkRepository, never()).findAll();
    }
}
