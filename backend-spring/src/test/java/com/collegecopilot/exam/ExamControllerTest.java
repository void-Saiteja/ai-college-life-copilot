package com.collegecopilot.exam;

import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.exam.dto.*;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ExamControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ExamService examService;

    @InjectMocks
    private ExamController examController;

    private JwtTokenProvider tokenProvider;
    private String validToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        validToken = tokenProvider.generateToken("user-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(examController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/exams with valid JWT returns 200 with summary and exam list")
    void testGetExamsSuccess() throws Exception {
        ExamMetrics metrics = new ExamMetrics(2, "VERY_SOON", "HIGH");
        ExamDto dto = new ExamDto("exm-1", "std-1", "sub-1", "CS201", "Data Structures",
                "Midterm Exam", "2026-10-10", "10:00 AM", "Hall A", "Modules 1-3",
                "Upcoming", "2026-10-01T00:00:00Z", metrics);

        ExamSummaryDto summary = new ExamSummaryDto(1, 1, 0, 1, 1, 0);
        ExamListResponse response = new ExamListResponse(true, summary, List.of(dto));

        when(examService.getExams("user-1", null)).thenReturn(response);

        mockMvc.perform(get("/api/exams")
                        .header("Authorization", "Bearer " + validToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.summary.total").value(1))
                .andExpect(jsonPath("$.summary.upcoming").value(1))
                .andExpect(jsonPath("$.data[0].id").value("exm-1"))
                .andExpect(jsonPath("$.data[0].title").value("Midterm Exam"))
                .andExpect(jsonPath("$.data[0].examStatus").value("VERY_SOON"))
                .andExpect(jsonPath("$.data[0].urgency").value("HIGH"));
    }

    @Test
    @DisplayName("GET /api/exams without JWT returns 401 Unauthorized")
    void testGetExamsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/exams")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized: Student authentication required"));
    }

    @Test
    @DisplayName("GET /api/exams/{id} returns single exam for owner")
    void testGetExamByIdSuccess() throws Exception {
        ExamMetrics metrics = new ExamMetrics(2, "VERY_SOON", "HIGH");
        ExamDto dto = new ExamDto("exm-1", "std-1", "sub-1", "CS201", "Data Structures",
                "Midterm Exam", "2026-10-10", "10:00 AM", "Hall A", "Modules 1-3",
                "Upcoming", "2026-10-01T00:00:00Z", metrics);

        when(examService.getExamById("user-1", "exm-1")).thenReturn(dto);

        mockMvc.perform(get("/api/exams/exm-1")
                        .header("Authorization", "Bearer " + validToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("exm-1"))
                .andExpect(jsonPath("$.data.title").value("Midterm Exam"));
    }

    @Test
    @DisplayName("GET /api/exams/{id} cross-student access returns 403 Forbidden")
    void testGetExamByIdForbidden() throws Exception {
        when(examService.getExamById("user-1", "exm-victim"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this exam record"));

        mockMvc.perform(get("/api/exams/exm-victim")
                        .header("Authorization", "Bearer " + validToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied: You do not own this exam record"));
    }

    @Test
    @DisplayName("GET /api/exams/{id} missing exam returns 404 Not Found")
    void testGetExamByIdNotFound() throws Exception {
        when(examService.getExamById("user-1", "exm-missing"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));

        mockMvc.perform(get("/api/exams/exm-missing")
                        .header("Authorization", "Bearer " + validToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Exam not found"));
    }

    @Test
    @DisplayName("POST /api/exams with valid payload creates exam and returns 201 Created")
    void testCreateExamSuccess() throws Exception {
        ExamMetrics metrics = new ExamMetrics(5, "UPCOMING", "MEDIUM");
        ExamDto created = new ExamDto("exm-new", "std-1", "sub-1", "CS201", "Data Structures",
                "Final Exam", "2026-10-25", "09:00 AM", "Hall B", "Full syllabus",
                "Upcoming", "2026-10-07T00:00:00Z", metrics);

        when(examService.createExam(eq("user-1"), any(ExamCreateRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Final Exam\", \"exam_date\": \"2026-10-25\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("exm-new"))
                .andExpect(jsonPath("$.data.title").value("Final Exam"));
    }

    @Test
    @DisplayName("POST /api/exams with empty fields returns 400 Bad Request")
    void testCreateExamValidationFailure() throws Exception {
        mockMvc.perform(post("/api/exams")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/exams/{id} updates exam and returns 200")
    void testUpdateExamSuccess() throws Exception {
        ExamMetrics metrics = new ExamMetrics(0, "COMPLETED", "COMPLETED");
        ExamDto updated = new ExamDto("exm-1", "std-1", "sub-1", "CS201", "Data Structures",
                "Midterm Exam", "2026-10-10", "10:00 AM", "Room 101", "Modules 1-3",
                "Completed", "2026-10-01T00:00:00Z", metrics);

        when(examService.updateExam(eq("user-1"), eq("exm-1"), any(ExamUpdateRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/exams/exm-1")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\": \"Room 101\", \"status\": \"Completed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.location").value("Room 101"))
                .andExpect(jsonPath("$.data.status").value("Completed"));
    }

    @Test
    @DisplayName("PUT /api/exams/{id} cross-student update returns 403 Forbidden")
    void testUpdateExamForbidden() throws Exception {
        when(examService.updateExam(eq("user-1"), eq("exm-victim"), any(ExamUpdateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this exam record"));

        mockMvc.perform(put("/api/exams/exm-victim")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"location\": \"Room 101\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied: You do not own this exam record"));
    }

    @Test
    @DisplayName("DELETE /api/exams/{id} deletes exam and returns 200")
    void testDeleteExamSuccess() throws Exception {
        when(examService.deleteExam("user-1", "exm-1"))
                .thenReturn(new ExamDeleteResponse(true, "Exam deleted successfully"));

        mockMvc.perform(delete("/api/exams/exm-1")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Exam deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/exams/{id} cross-student delete returns 403")
    void testDeleteExamForbidden() throws Exception {
        when(examService.deleteExam("user-1", "exm-victim"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this exam record"));

        mockMvc.perform(delete("/api/exams/exm-victim")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied: You do not own this exam record"));
    }

    @Test
    @DisplayName("POST /api/exams/advisor returns 200 with advisor recommendations")
    void testRunExamAdvisorSuccess() throws Exception {
        ExamAdvisorDataDto advisorData = new ExamAdvisorDataDto(
                "AI Exam Analysis complete for 1 upcoming exam(s).",
                "Focus on Midterm Exam today.",
                List.of(),
                false
        );

        when(examService.runExamAdvisor("user-1")).thenReturn(advisorData);

        mockMvc.perform(post("/api/exams/advisor")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary").value("AI Exam Analysis complete for 1 upcoming exam(s)."))
                .andExpect(jsonPath("$.data.recommendation").value("Focus on Midterm Exam today."))
                .andExpect(jsonPath("$.data.isFallback").value(false));
    }

    @Test
    @DisplayName("POST /api/exams/advisor without JWT returns 401")
    void testRunExamAdvisorUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/exams/advisor"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized: Student authentication required"));
    }
}
