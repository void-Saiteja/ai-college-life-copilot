package com.collegecopilot.studyplanner;

import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.security.JwtAuthenticationFilter;
import com.collegecopilot.security.JwtTokenProvider;
import com.collegecopilot.studyplanner.dto.*;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StudyPlannerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StudyPlannerService studyPlannerService;

    @InjectMocks
    private StudyPlannerController studyPlannerController;

    private JwtTokenProvider tokenProvider;
    private String validToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        validToken = tokenProvider.generateToken("user-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(studyPlannerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/planner returns 200 with plan and sessions when authenticated")
    void getStudyPlan_authenticated_returns200() throws Exception {
        StudyPlanRecordDto plan = new StudyPlanRecordDto("sp-1", "std-1", "Sprint 2026", "2026-10-07", "2026-10-14", new BigDecimal("15.0"), "Active", null);
        StudySessionDto session = new StudySessionDto("ss-1", "sp-1", "sub-1", "CS101", "Data Structures", "Trees", "Reason", "2026-10-07", "16:00", "18:00", "High", "Pending");
        PlannerGetResponse response = new PlannerGetResponse(true, new PlannerDataDto(plan, List.of(session), null));

        when(studyPlannerService.getStudyPlan(anyString())).thenReturn(response);

        mockMvc.perform(get("/api/planner")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.plan.id").value("sp-1"))
                .andExpect(jsonPath("$.data.plan.title").value("Sprint 2026"))
                .andExpect(jsonPath("$.data.sessions[0].id").value("ss-1"))
                .andExpect(jsonPath("$.data.sessions[0].topic").value("Trees"));
    }

    @Test
    @DisplayName("GET /api/planner returns 401 when unauthenticated")
    void getStudyPlan_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/planner"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/planner/generate returns 201 with generated plan when authenticated")
    void generateStudyPlan_authenticated_returns201() throws Exception {
        StudyPlanRecordDto plan = new StudyPlanRecordDto("sp-new", "std-1", "Exam Prep", "2026-10-07", "2026-10-14", new BigDecimal("21.0"), "Active", null);
        PlannerGenerateResponse response = new PlannerGenerateResponse(
                true,
                "AI Personalized Study Plan generated successfully",
                new PlannerDataDto(plan, List.of(), null)
        );

        when(studyPlannerService.generateStudyPlan(eq("std-1"), any())).thenReturn(response);

        mockMvc.perform(post("/api/planner/generate")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Exam Prep",
                                    "availableHoursPerDay": 3.0,
                                    "startTime": "16:00",
                                    "endTime": "21:00"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("AI Personalized Study Plan generated successfully"))
                .andExpect(jsonPath("$.data.plan.id").value("sp-new"));
    }

    @Test
    @DisplayName("POST /api/planner/generate returns 401 when unauthenticated")
    void generateStudyPlan_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/planner/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/planner/session/{id} returns 200 when updated successfully")
    void updateSessionStatus_success_returns200() throws Exception {
        StudySessionDto updated = new StudySessionDto("ss-1", "sp-1", "sub-1", "CS101", "DS", "Trees", "Reason", "2026-10-07", "16:00", "18:00", "High", "Completed");
        SessionUpdateResponse response = new SessionUpdateResponse(true, updated);

        when(studyPlannerService.updateSessionStatus(eq("std-1"), eq("ss-1"), any())).thenReturn(response);

        mockMvc.perform(put("/api/planner/session/ss-1")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"Completed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("Completed"));
    }

    @Test
    @DisplayName("PUT /api/planner/session/{id} returns 403 on cross-student ownership violation")
    void updateSessionStatus_crossStudentAccess_returns403() throws Exception {
        when(studyPlannerService.updateSessionStatus(eq("std-1"), eq("ss-1"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this study session"));

        mockMvc.perform(put("/api/planner/session/ss-1")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"Completed\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied: You do not own this study session"));
    }

    @Test
    @DisplayName("PUT /api/planner/session/{id} returns 404 when session does not exist")
    void updateSessionStatus_notFound_returns404() throws Exception {
        when(studyPlannerService.updateSessionStatus(eq("std-1"), eq("ss-999"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        mockMvc.perform(put("/api/planner/session/ss-999")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"Completed\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Session not found"));
    }

    @Test
    @DisplayName("PUT /api/planner/session/{id} returns 400 for invalid status value")
    void updateSessionStatus_invalidStatus_returns400() throws Exception {
        when(studyPlannerService.updateSessionStatus(eq("std-1"), eq("ss-1"), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status 'InvalidStatus'. Allowed statuses: Pending, In Progress, Completed, Skipped"));

        mockMvc.perform(put("/api/planner/session/ss-1")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"InvalidStatus\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid status 'InvalidStatus'. Allowed statuses: Pending, In Progress, Completed, Skipped"));
    }

    @Test
    @DisplayName("POST /api/planner/replan returns 200 with replanned sessions")
    void replanSchedule_success_returns200() throws Exception {
        StudySessionDto session = new StudySessionDto("ss-1", "sp-1", "sub-1", "CS101", "DS", "Trees", "Reason", "2026-10-08", "16:00", "18:00", "High", "Pending");
        PlannerReplanResponse response = new PlannerReplanResponse(
                true,
                "Replanned remaining sessions successfully! Completed sessions preserved.",
                List.of(session)
        );

        when(studyPlannerService.replanSchedule("std-1")).thenReturn(response);

        mockMvc.perform(post("/api/planner/replan")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Replanned remaining sessions successfully! Completed sessions preserved."))
                .andExpect(jsonPath("$.data[0].id").value("ss-1"));
    }

    @Test
    @DisplayName("POST /api/planner/replan returns 404 when no active plan exists")
    void replanSchedule_noActivePlan_returns404() throws Exception {
        when(studyPlannerService.replanSchedule("std-1"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "No active study plan to replan"));

        mockMvc.perform(post("/api/planner/replan")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("No active study plan to replan"));
    }

    @Test
    @DisplayName("POST /api/planner/replan returns 401 when unauthenticated")
    void replanSchedule_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/planner/replan"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
