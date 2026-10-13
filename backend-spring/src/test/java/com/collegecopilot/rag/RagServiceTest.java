package com.collegecopilot.rag;

import com.collegecopilot.rag.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private PdfExtractionService pdfExtractionService;

    @Mock
    private GeminiEmbeddingService embeddingService;

    @Mock
    private GeminiRagService geminiRagService;

    @Mock
    private RagRetrievalService ragRetrievalService;

    @InjectMocks
    private RagService ragService;

    private UserPrincipal adminPrincipal;
    private UserPrincipal studentPrincipal;

    @BeforeEach
    void setUp() {
        adminPrincipal = new UserPrincipal("u-admin", "Dean Smith", "admin@college.edu", "ADMIN", null, "adm-1");
        studentPrincipal = new UserPrincipal("u-student", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);
    }

    @Test
    @DisplayName("Admin can upload document with text content, creating chunks and embeddings")
    void uploadDocument_adminText_createsDocumentAndChunks() {
        when(pdfExtractionService.generateOverlappingChunks(any(), any(), any())).thenReturn(List.of(
                new ChunkDto("chk-1", "doc-1", "Rules", 1, 0, "College rules text", null, "m", 768, "v1", 0.0)
        ));
        when(embeddingService.generateSemanticEmbedding(any())).thenReturn(
                new GeminiEmbeddingService.EmbeddingResult(new double[768], "google", "gemini-embedding-2", 768, "v1")
        );

        DocumentUploadResponse response = ragService.uploadDocument(null, "Campus Rules", "This is direct policy text.", adminPrincipal);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("Campus Rules", response.getData().getTitle());
        verify(documentRepository, times(1)).save(any(Document.class));
        verify(documentChunkRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("Student attempting document upload throws 403 Forbidden")
    void uploadDocument_studentRole_throws403() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                ragService.uploadDocument(null, "Hacked Rules", "Illegal text", studentPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Administrator role required"));
        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin can delete document and associated chunks")
    void deleteDocument_adminRole_deletesSuccessfully() {
        DocumentDeleteResponse response = ragService.deleteDocument("doc-1", adminPrincipal);

        assertTrue(response.isSuccess());
        assertEquals("Document deleted successfully", response.getMessage());
        verify(documentChunkRepository, times(1)).deleteByDocumentId("doc-1");
        verify(documentRepository, times(1)).deleteById("doc-1");
    }

    @Test
    @DisplayName("Student attempting document delete throws 403 Forbidden")
    void deleteDocument_studentRole_throws403() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                ragService.deleteDocument("doc-1", studentPrincipal));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(documentRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("answerRAGQuery returns grounded answer with source citations when verification succeeds")
    void answerRAGQuery_verifiedFound_returnsAnswerAndSources() {
        ChunkDto c1 = new ChunkDto("chk-1", "doc-1", "College Policy 2026", 4, 0,
                "Attendance requirement is 75 percent minimum.", null, "gemini-embedding-2", 768, "v1", 0.92);

        when(ragRetrievalService.retrieveRelevantChunks(eq("attendance requirement"), eq(3), eq(0.55)))
                .thenReturn(List.of(c1));

        when(geminiRagService.answerAndVerifyDocumentQuery(eq("attendance requirement"), any()))
                .thenReturn(new GeminiRagService.GroundingVerificationResult(
                        true, "FOUND", "Minimum attendance is 75% per College Policy 2026 (Page 4).", false, null
                ));

        RAGQueryResponse response = ragService.answerRAGQuery("attendance requirement");

        assertTrue(response.isSuccess());
        assertTrue(response.getData().isFound());
        assertEquals("Minimum attendance is 75% per College Policy 2026 (Page 4).", response.getData().getAnswer());
        assertEquals(1, response.getData().getSources().size());
        assertEquals("College Policy 2026", response.getData().getSources().get(0).getDocumentTitle());
        assertEquals(4, response.getData().getSources().get(0).getPageNumber());
        assertEquals(0.92, response.getData().getSources().get(0).getSimilarityScore());
    }

    @Test
    @DisplayName("answerRAGQuery returns polite unknown message when no chunks meet similarity threshold")
    void answerRAGQuery_noRelevantChunks_returnsUnknownMessage() {
        when(ragRetrievalService.retrieveRelevantChunks(any(), anyInt(), anyDouble()))
                .thenReturn(Collections.emptyList());

        RAGQueryResponse response = ragService.answerRAGQuery("What is the policy on private helicopter parking?");

        assertTrue(response.isSuccess());
        assertFalse(response.getData().isFound());
        assertTrue(response.getData().getAnswer().contains("could not find an answer"));
        assertTrue(response.getData().getSources().isEmpty());
    }

    @Test
    @DisplayName("answerRAGQuery returns polite unknown message when verification status is NOT_FOUND (hallucination blocked)")
    void answerRAGQuery_verificationNotFound_returnsUnknownMessage() {
        ChunkDto c1 = new ChunkDto("chk-1", "doc-1", "Parking Guide", 1, 0, "Bicycle parking available.", null, "m", 768, "v1", 0.60);
        when(ragRetrievalService.retrieveRelevantChunks(any(), anyInt(), anyDouble())).thenReturn(List.of(c1));

        when(geminiRagService.answerAndVerifyDocumentQuery(any(), any()))
                .thenReturn(new GeminiRagService.GroundingVerificationResult(true, "NOT_FOUND", "", false, null));

        RAGQueryResponse response = ragService.answerRAGQuery("Can I land my helicopter?");

        assertTrue(response.isSuccess());
        assertFalse(response.getData().isFound());
        assertTrue(response.getData().getAnswer().contains("could not find an answer"));
    }
}
