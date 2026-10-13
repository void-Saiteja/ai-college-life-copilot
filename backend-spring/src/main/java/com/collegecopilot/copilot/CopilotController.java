package com.collegecopilot.copilot;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.copilot.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class CopilotController {

    private final CopilotService copilotService;

    public CopilotController(CopilotService copilotService) {
        this.copilotService = copilotService;
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        return principal;
    }

    // GET /api/chat/sessions, /api/copilot/sessions
    @GetMapping({"/api/chat/sessions", "/api/copilot/sessions"})
    public ResponseEntity<ChatSessionListResponse> getChatSessions() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(copilotService.getChatSessions(principal));
    }

    // GET /api/chat/sessions/{id}, /api/chat/sessions/{id}/messages, /api/copilot/sessions/{id}
    @GetMapping({
            "/api/chat/sessions/{id}",
            "/api/chat/sessions/{id}/messages",
            "/api/copilot/sessions/{id}",
            "/api/copilot/sessions/{id}/messages"
    })
    public ResponseEntity<ChatMessageListResponse> getSessionMessages(
            @PathVariable("id") String sessionId) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(copilotService.getSessionMessages(sessionId, principal));
    }

    // POST /api/chat/chat, /api/chat, /api/copilot/chat, /api/copilot
    @PostMapping({"/api/chat/chat", "/api/chat", "/api/copilot/chat", "/api/copilot"})
    public ResponseEntity<ChatResponse> postChatMessage(
            @RequestBody ChatRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(copilotService.postChatMessage(request, principal));
    }

    // DELETE /api/chat/sessions/{id}, /api/copilot/sessions/{id}
    @DeleteMapping({"/api/chat/sessions/{id}", "/api/copilot/sessions/{id}"})
    public ResponseEntity<ChatDeleteResponse> deleteChatSession(
            @PathVariable("id") String sessionId) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(copilotService.deleteChatSession(sessionId, principal));
    }

    // POST /api/ai/chat (support quick AI assistant companion used by AIStudyAssistantView)
    @PostMapping("/api/ai/chat")
    public ResponseEntity<QuickChatResponse> chatCopilotQuick(
            @RequestBody Map<String, Object> body) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        String message = body != null && body.get("message") != null ? body.get("message").toString() : null;
        return ResponseEntity.ok(copilotService.chatCopilotQuick(message, principal));
    }
}
