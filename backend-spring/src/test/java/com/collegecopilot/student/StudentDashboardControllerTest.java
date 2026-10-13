package com.collegecopilot.student;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.security.JwtAuthenticationFilter;
import com.collegecopilot.security.JwtTokenProvider;
import com.collegecopilot.student.dto.*;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StudentDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StudentDashboardService dashboardService;

    @InjectMocks
    private StudentDashboardController dashboardController;

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/student/dashboard with valid JWT returns 200 and matches dashboard contract")
    void testGetDashboardSuccess() throws Exception {
        String token = tokenProvider.generateToken("u-student-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);

        DashboardDataDto data = new DashboardDataDto();
        data.setStudent(new StudentProfileDto("std-1", "Alex Mercer", "alex@student.edu", 4, "Computer Science & Engineering", new BigDecimal("3.85")));
        data.setSubjects(List.of(new SubjectDto("sub-1", "CS 201", "Data Structures", "Dr. Turing", 4, "Computer Science")));
        data.setAttendance(List.of(new AttendanceDto("Data Structures", "CS 201", 28, 25, 89, "Good Standing")));
        data.setUpcomingAssignments(Collections.emptyList());
        data.setUpcomingExams(Collections.emptyList());
        data.setStudyPlan(new StudyPlanDto(null, Collections.emptyList()));
        data.setRecentQuizScore("80% in Data Structures");
        data.setQuizHistorySummary(new QuizHistorySummaryDto(1, Collections.emptyList(), null));
        data.setNotifications(List.of(new NotificationDto("n-1", "Notice", "Midterm coming up", "exam", false)));

        when(dashboardService.getStudentDashboard("u-student-1")).thenReturn(new StudentDashboardResponse(true, data));

        mockMvc.perform(get("/api/student/dashboard")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.student.id").value("std-1"))
                .andExpect(jsonPath("$.data.student.name").value("Alex Mercer"))
                .andExpect(jsonPath("$.data.student.email").value("alex@student.edu"))
                .andExpect(jsonPath("$.data.student.semester").value(4))
                .andExpect(jsonPath("$.data.subjects[0].code").value("CS 201"))
                .andExpect(jsonPath("$.data.attendance[0].percentage").value(89))
                .andExpect(jsonPath("$.data.attendance[0].status").value("Good Standing"))
                .andExpect(jsonPath("$.data.recentQuizScore").value("80% in Data Structures"))
                .andExpect(jsonPath("$.data.notifications[0].title").value("Notice"));
    }

    @Test
    @DisplayName("GET /api/student/dashboard without JWT returns 401 Unauthorized")
    void testGetDashboardUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/student/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized: Student authentication required"));
    }

    @Test
    @DisplayName("GET /api/student/dashboard with invalid JWT returns 401 Unauthorized")
    void testGetDashboardInvalidToken() throws Exception {
        mockMvc.perform(get("/api/student/dashboard")
                        .header("Authorization", "Bearer forged.or.expired.token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized: Student authentication required"));
    }

    @Test
    @DisplayName("GET /api/student/dashboard returns 404 when student record is missing for user")
    void testGetDashboardMissingStudentProfile() throws Exception {
        String token = tokenProvider.generateToken("u-nonstudent", "Admin User", "admin@college.edu", "ADMIN", null, "adm-1");

        when(dashboardService.getStudentDashboard("u-nonstudent"))
                .thenThrow(new AuthException(HttpStatus.NOT_FOUND, "Student profile not found for authenticated user"));

        mockMvc.perform(get("/api/student/dashboard")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Student profile not found for authenticated user"));
    }

    @Test
    @DisplayName("Client supplying a spoofed studentId in query or header is ignored (ownership strictly from JWT)")
    void testClientSuppliedStudentIdIsIgnored() throws Exception {
        String token = tokenProvider.generateToken("u-student-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);

        DashboardDataDto data = new DashboardDataDto();
        data.setStudent(new StudentProfileDto("std-1", "Alex Mercer", "alex@student.edu", 4, "CS", new BigDecimal("3.85")));
        when(dashboardService.getStudentDashboard("u-student-1")).thenReturn(new StudentDashboardResponse(true, data));

        // Malicious client tries to access victim's std-2 via query parameter
        mockMvc.perform(get("/api/student/dashboard?studentId=std-2")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.student.id").value("std-1"));
    }
}
