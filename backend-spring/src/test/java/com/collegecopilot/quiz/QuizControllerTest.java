package com.collegecopilot.quiz;

import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.quiz.dto.*;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QuizControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QuizService quizService;

    @InjectMocks
    private QuizController quizController;

    private JwtTokenProvider tokenProvider;
    private String validToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        validToken = tokenProvider.generateToken("user-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(quizController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/quizzes/generate returns 201 with generated quiz when authenticated")
    void generateQuiz_authenticated_returns201() throws Exception {
        QuizGeneratedDataDto data = new QuizGeneratedDataDto(
                "qz-1", "CS 201", "Trees", "Medium", 1,
                List.of(new QuizQuestionDto(1, "Question?", List.of("A", "B"))), false
        );
        when(quizService.generateQuiz(eq("std-1"), any())).thenReturn(new QuizGenerateResponse(true, data));

        mockMvc.perform(post("/api/quizzes/generate")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"CS 201\",\"topic\":\"Trees\",\"difficulty\":\"Medium\",\"questionCount\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.quizId").value("qz-1"))
                .andExpect(jsonPath("$.data.topic").value("Trees"));
    }

    @Test
    @DisplayName("POST /api/quizzes/generate returns 401 when unauthenticated")
    void generateQuiz_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/quizzes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/quiz/generate (singular path) returns 201 when authenticated")
    void generateQuiz_singularPath_returns201() throws Exception {
        QuizGeneratedDataDto data = new QuizGeneratedDataDto("qz-1", "CS 201", "Trees", "Medium", 1, List.of(), false);
        when(quizService.generateQuiz(eq("std-1"), any())).thenReturn(new QuizGenerateResponse(true, data));

        mockMvc.perform(post("/api/quiz/generate")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/quizzes returns 200 with attempts and quizzes history")
    void getQuizzes_authenticated_returns200() throws Exception {
        QuizAttemptDto attempt = new QuizAttemptDto("qa-1", "qz-1", "std-1", "CS 201", "Trees", 5, 5, new BigDecimal("100.00"), "2026-10-07");
        QuizHistoryDataDto data = new QuizHistoryDataDto(List.of(attempt), List.of());
        when(quizService.getQuizzesAndHistory("std-1")).thenReturn(new QuizHistoryResponse(true, data));

        mockMvc.perform(get("/api/quizzes")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attempts[0].id").value("qa-1"));
    }

    @Test
    @DisplayName("GET /api/quizzes/history returns 200 with attempts and quizzes history")
    void getQuizHistory_authenticated_returns200() throws Exception {
        QuizHistoryDataDto data = new QuizHistoryDataDto(List.of(), List.of());
        when(quizService.getQuizzesAndHistory("std-1")).thenReturn(new QuizHistoryResponse(true, data));

        mockMvc.perform(get("/api/quizzes/history")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/quizzes/{id} returns 200 with quiz details when owned by student")
    void getQuizById_success_returns200() throws Exception {
        QuizRecordDto record = new QuizRecordDto("qz-1", "std-1", "CS 201", "Trees", "Medium", 2, List.of(), "Pending", null, null, "2026-10-07", null);
        when(quizService.getQuizById("std-1", "qz-1")).thenReturn(new QuizDetailResponse(true, record));

        mockMvc.perform(get("/api/quizzes/qz-1")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("qz-1"));
    }

    @Test
    @DisplayName("GET /api/quizzes/{id} returns 403 on cross-student access")
    void getQuizById_crossStudent_returns403() throws Exception {
        when(quizService.getQuizById("std-1", "qz-2"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this quiz"));

        mockMvc.perform(get("/api/quizzes/qz-2")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied: You do not own this quiz"));
    }

    @Test
    @DisplayName("GET /api/quizzes/{id} returns 404 when quiz not found")
    void getQuizById_notFound_returns404() throws Exception {
        when(quizService.getQuizById("std-1", "qz-unknown"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

        mockMvc.perform(get("/api/quizzes/qz-unknown")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Quiz not found"));
    }

    @Test
    @DisplayName("POST /api/quizzes/{id}/submit returns 200 with evaluation score")
    void submitQuiz_success_returns200() throws Exception {
        QuizSubmissionResultDto result = new QuizSubmissionResultDto(
                "qa-1", "qz-1", "CS 201", "Trees", 2, 2, 2, 0, 0, 100.0, "STRONG",
                List.of(), List.of(), List.of(), "Excellent!"
        );
        when(quizService.submitQuiz(eq("std-1"), eq("qz-1"), any())).thenReturn(new QuizSubmitResponse(true, result));

        mockMvc.perform(post("/api/quizzes/qz-1/submit")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":{\"1\":0,\"2\":1}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.score").value(2))
                .andExpect(jsonPath("$.data.percentage").value(100.0));
    }

    @Test
    @DisplayName("POST /api/quizzes/{id}/submit returns 409 Conflict when quiz has already been submitted")
    void submitQuiz_duplicateSubmission_returns409() throws Exception {
        when(quizService.submitQuiz(eq("std-1"), eq("qz-1"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "This quiz has already been submitted and completed"));

        mockMvc.perform(post("/api/quizzes/qz-1/submit")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":{\"1\":0}}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("This quiz has already been submitted and completed"));
    }

    @Test
    @DisplayName("POST /api/quizzes/submit returns 200 when submitting directly")
    void submitQuizDirect_success_returns200() throws Exception {
        QuizSubmissionResultDto result = new QuizSubmissionResultDto(
                "qa-1", null, "CS 201", "Trees", 1, 1, 1, 0, 0, 100.0, "STRONG",
                List.of(), List.of(), List.of(), "Good job!"
        );
        when(quizService.submitQuiz(eq("std-1"), isNull(), any())).thenReturn(new QuizSubmitResponse(true, result));

        mockMvc.perform(post("/api/quizzes/submit")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":{\"1\":0}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/quizzes/{id} returns 200 when deleting quiz")
    void deleteQuiz_success_returns200() throws Exception {
        when(quizService.deleteQuiz("std-1", "qz-1")).thenReturn(new QuizDeleteResponse(true, "Quiz deleted successfully"));

        mockMvc.perform(delete("/api/quizzes/qz-1")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Quiz deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/quizzes/{id} returns 403 on cross-student deletion")
    void deleteQuiz_crossStudent_returns403() throws Exception {
        when(quizService.deleteQuiz("std-1", "qz-2"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this quiz"));

        mockMvc.perform(delete("/api/quizzes/qz-2")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/quizzes/advisor returns 200 with advice")
    void runQuizAdvisor_authenticated_returns200() throws Exception {
        QuizAdvisorDataDto advice = new QuizAdvisorDataDto("Summary", "Recommendation", List.of("Trees"), List.of(), false);
        when(quizService.runQuizAdvisor("std-1")).thenReturn(new QuizAdvisorResponse(true, advice));

        mockMvc.perform(post("/api/quizzes/advisor")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary").value("Summary"))
                .andExpect(jsonPath("$.data.weakTopics[0]").value("Trees"));
    }

    @Test
    @DisplayName("POST /api/quizzes/advisor returns 401 when unauthenticated")
    void runQuizAdvisor_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/quizzes/advisor"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
