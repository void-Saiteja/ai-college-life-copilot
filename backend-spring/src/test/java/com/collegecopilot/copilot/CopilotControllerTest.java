package com.collegecopilot.copilot;

import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.copilot.dto.*;
import com.collegecopilot.security.JwtAuthenticationFilter;
import com.collegecopilot.security.JwtTokenProvider;
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
class CopilotControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CopilotService copilotService;

    @InjectMocks
    private CopilotController copilotController;

    private JwtTokenProvider tokenProvider;
    private String studentTokenA;
    private String studentTokenB;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        studentTokenA = tokenProvider.generateToken("user-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);
        studentTokenB = tokenProvider.generateToken("user-2", "Bob Student", "bob@student.edu", "STUDENT", "std-2", null);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(copilotController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/chat/sessions returns 200 with session list when authenticated")
    void getChatSessions_authenticated_returns200() throws Exception {
        ChatSessionDto sessionDto = new ChatSessionDto("cs-1", "std-1", "Study Guidance", "2026-10-07T10:00:00Z", "2026-10-07T10:00:00Z");
        when(copilotService.getChatSessions(any())).thenReturn(new ChatSessionListResponse(true, List.of(sessionDto)));

        mockMvc.perform(get("/api/chat/sessions")
                        .header("Authorization", "Bearer " + studentTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("cs-1"))
                .andExpect(jsonPath("$.data[0].student_id").value("std-1"))
                .andExpect(jsonPath("$.data[0].title").value("Study Guidance"));
    }

    @Test
    @DisplayName("GET /api/chat/sessions returns 401 when unauthenticated")
    void getChatSessions_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/chat/sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/copilot/sessions alias returns 200 when authenticated")
    void getChatSessions_aliasRoute_returns200() throws Exception {
        when(copilotService.getChatSessions(any())).thenReturn(new ChatSessionListResponse(true, Collections.emptyList()));

        mockMvc.perform(get("/api/copilot/sessions")
                        .header("Authorization", "Bearer " + studentTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/chat/sessions/{id} returns 200 with messages for session owner")
    void getSessionMessages_authenticatedOwner_returns200() throws Exception {
        ChatMessageDto userMsg = new ChatMessageDto("cm-1", "cs-1", "user", "What is due tomorrow?", Collections.emptyList(), "2026-10-07T10:00:00Z");
        ChatMessageDto assistantMsg = new ChatMessageDto("cm-2", "cs-1", "assistant", "You have DSA assignment due tomorrow.", Collections.emptyList(), "2026-10-07T10:00:05Z");

        when(copilotService.getSessionMessages(eq("cs-1"), any())).thenReturn(new ChatMessageListResponse(true, List.of(userMsg, assistantMsg)));

        mockMvc.perform(get("/api/chat/sessions/cs-1")
                        .header("Authorization", "Bearer " + studentTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sender").value("user"))
                .andExpect(jsonPath("$.data[0].content").value("What is due tomorrow?"))
                .andExpect(jsonPath("$.data[1].sender").value("assistant"))
                .andExpect(jsonPath("$.data[1].content").value("You have DSA assignment due tomorrow."));
    }

    @Test
    @DisplayName("GET /api/chat/sessions/{id} returns 403 when another student attempts to read session")
    void getSessionMessages_crossStudentAccess_returns403() throws Exception {
        when(copilotService.getSessionMessages(eq("cs-1"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this chat session"));

        mockMvc.perform(get("/api/chat/sessions/cs-1")
                        .header("Authorization", "Bearer " + studentTokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied: You do not own this chat session"));
    }

    @Test
    @DisplayName("GET /api/chat/sessions/{id} returns 404 when session not found")
    void getSessionMessages_missingSession_returns404() throws Exception {
        when(copilotService.getSessionMessages(eq("cs-999"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat session not found"));

        mockMvc.perform(get("/api/chat/sessions/cs-999")
                        .header("Authorization", "Bearer " + studentTokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Chat session not found"));
    }

    @Test
    @DisplayName("POST /api/chat/chat returns 200 with assistant reply when authenticated")
    void postChatMessage_authenticated_returns200() throws Exception {
        ChatMessageDto userMsg = new ChatMessageDto("cm-1", "cs-1", "user", "How is my attendance?", Collections.emptyList(), "2026-10-07T10:00:00Z");
        ChatMessageDto assistantMsg = new ChatMessageDto("cm-2", "cs-1", "assistant", "Your attendance is at 88%.", Collections.emptyList(), "2026-10-07T10:00:02Z");
        ChatResponse response = new ChatResponse(true, new ChatResponse.ChatResponseData("cs-1", userMsg, assistantMsg));

        when(copilotService.postChatMessage(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/chat/chat")
                        .header("Authorization", "Bearer " + studentTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"cs-1\",\"message\":\"How is my attendance?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value("cs-1"))
                .andExpect(jsonPath("$.data.userMessage.content").value("How is my attendance?"))
                .andExpect(jsonPath("$.data.assistantMessage.content").value("Your attendance is at 88%."));
    }

    @Test
    @DisplayName("POST /api/chat/chat returns 200 with offline fallback when Gemini service encounters 503")
    void postChatMessage_gemini503Fallback_returns200WithoutStackOverflow() throws Exception {
        String fallbackContent = "📋 **Academic Copilot (Offline Mode)**\n\n- Cumulative GPA: 3.85";
        ChatMessageDto userMsg = new ChatMessageDto("cm-1", "cs-1", "user", "What is my GPA?", Collections.emptyList(), "2026-10-07T10:00:00Z");
        ChatMessageDto assistantMsg = new ChatMessageDto("cm-2", "cs-1", "assistant", fallbackContent, Collections.emptyList(), "2026-10-07T10:00:02Z");
        ChatResponse response = new ChatResponse(true, new ChatResponse.ChatResponseData("cs-1", userMsg, assistantMsg));

        when(copilotService.postChatMessage(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/chat/chat")
                        .header("Authorization", "Bearer " + studentTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What is my GPA?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assistantMessage.content").value(org.hamcrest.Matchers.containsString("Academic Copilot (Offline Mode)")))
                .andExpect(jsonPath("$.data.assistantMessage.content").value(org.hamcrest.Matchers.containsString("3.85")));
    }

    @Test
    @DisplayName("POST /api/chat/chat returns 401 when unauthenticated")
    void postChatMessage_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/chat/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/chat/chat returns 400 when message is empty")
    void postChatMessage_emptyMessage_returns400() throws Exception {
        when(copilotService.postChatMessage(any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content is required"));

        mockMvc.perform(post("/api/chat/chat")
                        .header("Authorization", "Bearer " + studentTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Message content is required"));
    }

    @Test
    @DisplayName("DELETE /api/chat/sessions/{id} returns 200 for session owner")
    void deleteChatSession_authenticatedOwner_returns200() throws Exception {
        when(copilotService.deleteChatSession(eq("cs-1"), any()))
                .thenReturn(new ChatDeleteResponse(true, "Chat conversation deleted"));

        mockMvc.perform(delete("/api/chat/sessions/cs-1")
                        .header("Authorization", "Bearer " + studentTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Chat conversation deleted"));
    }

    @Test
    @DisplayName("DELETE /api/chat/sessions/{id} returns 403 for non-owner student")
    void deleteChatSession_crossStudentAccess_returns403() throws Exception {
        when(copilotService.deleteChatSession(eq("cs-1"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this chat session"));

        mockMvc.perform(delete("/api/chat/sessions/cs-1")
                        .header("Authorization", "Bearer " + studentTokenB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/ai/chat returns 200 with quick reply when authenticated")
    void chatCopilotQuick_authenticated_returns200() throws Exception {
        when(copilotService.chatCopilotQuick(eq("study tip"), any()))
                .thenReturn(new QuickChatResponse(true, "Pomodoro tip: 25 min study + 5 min break", "2026-10-07T10:00:00Z"));

        mockMvc.perform(post("/api/ai/chat")
                        .header("Authorization", "Bearer " + studentTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"study tip\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.reply").value("Pomodoro tip: 25 min study + 5 min break"));
    }

    @Test
    @DisplayName("POST /api/ai/chat returns 401 when unauthenticated")
    void chatCopilotQuick_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"study tip\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
