package com.collegecopilot.rag;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.rag.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class RagController {

    private final RagService ragService;

    @Autowired
    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Authentication required");
        }
        return principal;
    }

    // GET /api/documents
    @GetMapping("/api/documents")
    public ResponseEntity<DocumentListResponse> getDocuments() {
        getAuthenticatedPrincipal();
        return ResponseEntity.ok(ragService.getDocuments());
    }

    // POST /api/documents/upload (Multipart upload for PDF files)
    @PostMapping(value = "/api/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadMultipartDocument(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "text", required = false) String text) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        DocumentUploadResponse response = ragService.uploadDocument(file, title, text, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/documents/upload (JSON upload for direct text)
    @PostMapping(value = "/api/documents/upload", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadJsonDocument(
            @RequestBody Map<String, Object> body) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String title = body != null && body.get("title") != null ? body.get("title").toString() : null;
        String text = body != null && body.get("text") != null ? body.get("text").toString() : null;

        DocumentUploadResponse response = ragService.uploadDocument(null, title, text, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/documents/query
    @PostMapping("/api/documents/query")
    public ResponseEntity<RAGQueryResponse> queryRAG(
            @RequestBody RAGQueryRequest request) {
        getAuthenticatedPrincipal();
        String query = request != null ? request.getQuery() : null;
        return ResponseEntity.ok(ragService.answerRAGQuery(query));
    }

    // DELETE /api/documents/{id}
    @DeleteMapping("/api/documents/{id}")
    public ResponseEntity<DocumentDeleteResponse> deleteDocument(
            @PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(ragService.deleteDocument(id, principal));
    }
}
