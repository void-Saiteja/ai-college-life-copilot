package com.collegecopilot.rag;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.rag.dto.*;
import com.collegecopilot.security.JwtAuthenticationFilter;
import com.collegecopilot.security.JwtTokenProvider;
import com.collegecopilot.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RagControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RagService ragService;

    @InjectMocks
    private RagController ragController;

    private JwtTokenProvider tokenProvider;
    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        adminToken = tokenProvider.generateToken("admin-1", "Admin User", "admin@college.edu", "ADMIN", null, null);
        studentToken = tokenProvider.generateToken("user-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(ragController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/documents - Returns list of documents when authenticated")
    void testGetDocuments_Authenticated() throws Exception {
        DocumentDto doc = new DocumentDto("doc-1", "Handbook", "handbook.pdf", 1024, "admin-1", 5, "2026-03-30T10:00:00");
        DocumentListResponse listResponse = new DocumentListResponse(true, List.of(doc));
        when(ragService.getDocuments()).thenReturn(listResponse);

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("doc-1"))
                .andExpect(jsonPath("$.data[0].title").value("Handbook"));
    }

    @Test
    @DisplayName("GET /api/documents - Returns 401 when unauthenticated")
    void testGetDocuments_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/documents/upload - Multipart upload succeeds for ADMIN")
    void testUploadMultipart_AsAdmin() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "policy.pdf", "application/pdf", "%PDF-1.4 test".getBytes());
        DocumentDto doc = new DocumentDto("doc-2", "Policy", "policy.pdf", (int) file.getSize(), "admin-1", 3, "2026-03-30T10:00:00");
        DocumentUploadResponse uploadResponse = new DocumentUploadResponse(true, "Document uploaded and indexed successfully", doc);
        when(ragService.uploadDocument(any(), eq("Policy"), any(), any(UserPrincipal.class))).thenReturn(uploadResponse);

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("title", "Policy")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document uploaded and indexed successfully"))
                .andExpect(jsonPath("$.data.id").value("doc-2"));
    }

    @Test
    @DisplayName("POST /api/documents/upload - Multipart upload rejected (403) for STUDENT")
    void testUploadMultipart_AsStudent_Forbidden() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "policy.pdf", "application/pdf", "%PDF-1.4 test".getBytes());
        when(ragService.uploadDocument(any(), eq("Policy"), any(), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator role required"));

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("title", "Policy")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/documents/upload - JSON text upload succeeds for ADMIN")
    void testUploadJson_AsAdmin() throws Exception {
        DocumentDto doc = new DocumentDto("doc-3", "Manual", "manual.txt", 100, "admin-1", 1, "2026-03-30T10:00:00");
        DocumentUploadResponse uploadResponse = new DocumentUploadResponse(true, "Document indexed successfully", doc);
        when(ragService.uploadDocument(any(), eq("Manual"), eq("This is raw text content."), any(UserPrincipal.class)))
                .thenReturn(uploadResponse);

        String jsonPayload = """
                {
                    "title": "Manual",
                    "text": "This is raw text content."
                }
                """;

        mockMvc.perform(post("/api/documents/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("doc-3"));
    }

    @Test
    @DisplayName("POST /api/documents/upload - JSON text rejected (403) for STUDENT")
    void testUploadJson_AsStudent_Forbidden() throws Exception {
        when(ragService.uploadDocument(any(), eq("Manual"), eq("This is raw text content."), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator role required"));

        String jsonPayload = """
                {
                    "title": "Manual",
                    "text": "This is raw text content."
                }
                """;

        mockMvc.perform(post("/api/documents/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/documents/query - Successful RAG query for authenticated student")
    void testQuery_Authenticated() throws Exception {
        RAGSourceDto source = new RAGSourceDto("Handbook", 3, 0.88, "Passing grade is 50%");
        RAGResultDto result = new RAGResultDto(true, "Passing grade is 50%", List.of(source));
        RAGQueryResponse queryResponse = new RAGQueryResponse(true, result);
        when(ragService.answerRAGQuery("What is the passing grade?")).thenReturn(queryResponse);

        String queryJson = """
                {
                    "query": "What is the passing grade?"
                }
                """;

        mockMvc.perform(post("/api/documents/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryJson)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").value("Passing grade is 50%"))
                .andExpect(jsonPath("$.data.sources[0].documentTitle").value("Handbook"))
                .andExpect(jsonPath("$.data.sources[0].pageNumber").value(3))
                .andExpect(jsonPath("$.data.sources[0].similarityScore").value(0.88));
    }

    @Test
    @DisplayName("POST /api/documents/query - Rejects blank query with 400 Bad Request")
    void testQuery_BlankQuery_BadRequest() throws Exception {
        when(ragService.answerRAGQuery("   "))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query is required"));

        String queryJson = """
                {
                    "query": "   "
                }
                """;

        mockMvc.perform(post("/api/documents/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryJson)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/documents/query - Returns 401 when unauthenticated")
    void testQuery_Unauthenticated() throws Exception {
        String queryJson = """
                {
                    "query": "What is the passing grade?"
                }
                """;

        mockMvc.perform(post("/api/documents/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(queryJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/documents/{id} - Deletes document as ADMIN")
    void testDelete_AsAdmin() throws Exception {
        DocumentDeleteResponse deleteResponse = new DocumentDeleteResponse(true, "Document deleted successfully");
        when(ragService.deleteDocument(eq("doc-1"), any(UserPrincipal.class))).thenReturn(deleteResponse);

        mockMvc.perform(delete("/api/documents/doc-1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Document deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/documents/{id} - Rejected (403) for STUDENT")
    void testDelete_AsStudent_Forbidden() throws Exception {
        when(ragService.deleteDocument(eq("doc-1"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Administrator role required"));

        mockMvc.perform(delete("/api/documents/doc-1")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/documents/{id} - Returns 401 when unauthenticated")
    void testDelete_Unauthenticated() throws Exception {
        mockMvc.perform(delete("/api/documents/doc-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/documents/{id} - Returns 404 when document not found")
    void testDelete_NotFound() throws Exception {
        when(ragService.deleteDocument(eq("non-existent"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        mockMvc.perform(delete("/api/documents/non-existent")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
