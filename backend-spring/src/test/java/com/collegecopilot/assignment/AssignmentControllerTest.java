package com.collegecopilot.assignment;

import com.collegecopilot.assignment.dto.*;
import com.collegecopilot.config.GlobalExceptionHandler;
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
class AssignmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AssignmentService assignmentService;

    @InjectMocks
    private AssignmentController assignmentController;

    private JwtTokenProvider tokenProvider;
    private String validToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        validToken = tokenProvider.generateToken("user-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(assignmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/assignments with valid JWT returns 200 with summary and assignment list")
    void testGetAssignmentsSuccess() throws Exception {
        AssignmentMetrics metrics = new AssignmentMetrics(3, "DUE_SOON", "HIGH");
        AssignmentDto dto = new AssignmentDto("asg-1", "std-1", "sub-1", "CS201", "Data Structures",
                "AVL Tree", "Implement balancing", "2026-10-10", "Pending", "High", 3.0, "Assignment",
                "2026-10-01T00:00:00Z", null, metrics);

        AssignmentSummaryDto summary = new AssignmentSummaryDto(1, 1, 0, 0, 1);
        AssignmentListResponse response = new AssignmentListResponse(true, summary, List.of(dto));

        when(assignmentService.getAssignments("user-1", null)).thenReturn(response);

        mockMvc.perform(get("/api/assignments")
                        .header("Authorization", "Bearer " + validToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.summary.total").value(1))
                .andExpect(jsonPath("$.summary.pending").value(1))
                .andExpect(jsonPath("$.data[0].id").value("asg-1"))
                .andExpect(jsonPath("$.data[0].title").value("AVL Tree"))
                .andExpect(jsonPath("$.data[0].deadlineStatus").value("DUE_SOON"))
                .andExpect(jsonPath("$.data[0].urgency").value("HIGH"));
    }

    @Test
    @DisplayName("GET /api/assignments without JWT returns 401 Unauthorized")
    void testGetAssignmentsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/assignments")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized: Student authentication required"));
    }

    @Test
    @DisplayName("GET /api/assignments/{id} returns single assignment for owner")
    void testGetAssignmentByIdSuccess() throws Exception {
        AssignmentMetrics metrics = new AssignmentMetrics(3, "DUE_SOON", "HIGH");
        AssignmentDto dto = new AssignmentDto("asg-1", "std-1", "sub-1", "CS201", "Data Structures",
                "AVL Tree", "Implement balancing", "2026-10-10", "Pending", "High", 3.0, "Assignment",
                "2026-10-01T00:00:00Z", null, metrics);

        when(assignmentService.getAssignmentById("user-1", "asg-1")).thenReturn(dto);

        mockMvc.perform(get("/api/assignments/asg-1")
                        .header("Authorization", "Bearer " + validToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("asg-1"))
                .andExpect(jsonPath("$.data.title").value("AVL Tree"));
    }

    @Test
    @DisplayName("GET /api/assignments/{id} cross-student access returns 403 Forbidden")
    void testGetAssignmentByIdForbidden() throws Exception {
        when(assignmentService.getAssignmentById("user-1", "asg-victim"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this assignment"));

        mockMvc.perform(get("/api/assignments/asg-victim")
                        .header("Authorization", "Bearer " + validToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied: You do not own this assignment"));
    }

    @Test
    @DisplayName("GET /api/assignments/{id} missing assignment returns 404 Not Found")
    void testGetAssignmentByIdNotFound() throws Exception {
        when(assignmentService.getAssignmentById("user-1", "asg-missing"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        mockMvc.perform(get("/api/assignments/asg-missing")
                        .header("Authorization", "Bearer " + validToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Assignment not found"));
    }

    @Test
    @DisplayName("POST /api/assignments with valid payload creates assignment and returns 201")
    void testCreateAssignmentSuccess() throws Exception {
        AssignmentMetrics metrics = new AssignmentMetrics(5, "UPCOMING", "MEDIUM");
        AssignmentDto created = new AssignmentDto("asg-new", "std-1", "sub-1", "CS201", "Data Structures",
                "Build B-Tree", "B-tree desc", "2026-10-12", "Pending", "Medium", 2.0, "Assignment",
                "2026-10-07T00:00:00Z", null, metrics);

        when(assignmentService.createAssignment(eq("user-1"), any(AssignmentCreateRequest.class)))
                .thenReturn(created);

        mockMvc.perform(post("/api/assignments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Build B-Tree\", \"course\": \"CS201\", \"dueDate\": \"2026-10-12\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("asg-new"))
                .andExpect(jsonPath("$.data.title").value("Build B-Tree"));
    }

    @Test
    @DisplayName("POST /api/assignments with empty title returns 400 Bad Request")
    void testCreateAssignmentValidationFailure() throws Exception {
        mockMvc.perform(post("/api/assignments")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/assignments/{id} updates assignment and returns 200")
    void testUpdateAssignmentSuccess() throws Exception {
        AssignmentMetrics metrics = new AssignmentMetrics(0, "COMPLETED", "COMPLETED");
        AssignmentDto updated = new AssignmentDto("asg-1", "std-1", "sub-1", "CS201", "Data Structures",
                "AVL Tree", "Done", "2026-10-10", "Completed", "High", 3.0, "Assignment",
                "2026-10-01T00:00:00Z", "2026-10-07T00:00:00Z", metrics);

        when(assignmentService.updateAssignment(eq("user-1"), eq("asg-1"), any(AssignmentUpdateRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/assignments/asg-1")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"Completed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("Completed"))
                .andExpect(jsonPath("$.data.deadlineStatus").value("COMPLETED"));
    }

    @Test
    @DisplayName("PUT /api/assignments/{id} cross-student update returns 403")
    void testUpdateAssignmentForbidden() throws Exception {
        when(assignmentService.updateAssignment(eq("user-1"), eq("asg-victim"), any(AssignmentUpdateRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this assignment"));

        mockMvc.perform(put("/api/assignments/asg-victim")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"Completed\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied: You do not own this assignment"));
    }

    @Test
    @DisplayName("DELETE /api/assignments/{id} deletes assignment and returns 200")
    void testDeleteAssignmentSuccess() throws Exception {
        when(assignmentService.deleteAssignment("user-1", "asg-1"))
                .thenReturn(new AssignmentDeleteResponse(true, "Assignment deleted successfully"));

        mockMvc.perform(delete("/api/assignments/asg-1")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Assignment deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/assignments/{id} cross-student delete returns 403")
    void testDeleteAssignmentForbidden() throws Exception {
        when(assignmentService.deleteAssignment("user-1", "asg-victim"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this assignment"));

        mockMvc.perform(delete("/api/assignments/asg-victim")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied: You do not own this assignment"));
    }

    @Test
    @DisplayName("POST /api/assignments/advisor returns 200 with prioritization data")
    void testRunAssignmentAdvisorSuccess() throws Exception {
        AdvisorDataDto advisorData = new AdvisorDataDto(
                "AI Analysis complete for 1 pending item(s).",
                "Focus on AVL Tree first.",
                List.of(),
                false
        );

        when(assignmentService.runAssignmentAdvisor("user-1")).thenReturn(advisorData);

        mockMvc.perform(post("/api/assignments/advisor")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary").value("AI Analysis complete for 1 pending item(s)."))
                .andExpect(jsonPath("$.data.recommendation").value("Focus on AVL Tree first."))
                .andExpect(jsonPath("$.data.isFallback").value(false));
    }

    @Test
    @DisplayName("POST /api/assignments/advisor without JWT returns 401")
    void testRunAssignmentAdvisorUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/assignments/advisor"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized: Student authentication required"));
    }
}
