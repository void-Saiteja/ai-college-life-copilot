package com.collegecopilot.admin;

import com.collegecopilot.admin.dto.*;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private JwtTokenProvider tokenProvider;
    private String adminToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        adminToken = tokenProvider.generateToken("u-admin-1", "Dean Rogers", "dean@college.edu", "ADMIN", null, "adm-1");
        studentToken = tokenProvider.generateToken("u-student-1", "Alex Student", "alex@college.edu", "STUDENT", "std-1", null);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 1. GET /api/admin/dashboard
    @Test
    @DisplayName("GET /api/admin/dashboard returns 401 when unauthenticated")
    void getDashboard_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/admin/dashboard returns 403 when student JWT is used")
    void getDashboard_studentJwt_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Forbidden: Access restricted to [ADMIN]"));
    }

    @Test
    @DisplayName("GET /api/admin/dashboard returns 200 with dashboard data for admin")
    void getDashboard_admin_returns200() throws Exception {
        AdminDashboardDataDto stats = new AdminDashboardDataDto(
                new AdminDashboardDataDto.StudentsStats(100, 90),
                new AdminDashboardDataDto.AttendanceStats(90, 84.5, 4, 8),
                new AdminDashboardDataDto.AssignmentsStats(40, 15, 25, 3, 5),
                new AdminDashboardDataDto.ExamsStats(8, 4, 1, 3),
                new AdminDashboardDataDto.QuizzesStats(50, 78.2, 5, 2),
                new AdminDashboardDataDto.CareerStats(70, 12, List.of(new AdminDashboardDataDto.SkillGapCount("SQL", 5))),
                new AdminDashboardDataDto.DocumentsStats(10, 100, "Google Gemini gemini-embedding-2", "Synchronized")
        );
        when(adminService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.students.totalStudents").value(100))
                .andExpect(jsonPath("$.data.attendance.averageAttendance").value(84.5))
                .andExpect(jsonPath("$.data.documents.indexingStatus").value("Synchronized"));
    }

    // 2. GET /api/admin/stats
    @Test
    @DisplayName("GET /api/admin/stats returns 403 for student and 200 for admin")
    void getStats_rolesVerification() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        AdminStatsDataDto stats = new AdminStatsDataDto(
                50, 5, 10, 20, 4, 15, List.of(), List.of()
        );
        when(adminService.getAdminStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalStudents").value(50));
    }

    // 3. GET /api/admin/students
    @Test
    @DisplayName("GET /api/admin/students returns 200 with count and does not expose passwords")
    void getStudents_admin_returnsListAndOmitSecrets() throws Exception {
        AdminStudentSummaryDto s1 = new AdminStudentSummaryDto(
                "std-1", "u-1", "Alice", "alice@college.edu", "ST101", 2, "CS",
                new BigDecimal("3.80"),
                new AdminStudentSummaryDto.AttendanceSummary(30, 28, 93.33, "SAFE", "ON_TRACK"),
                2, 0, 1, 5, 88.0, "Full Stack Developer", 85.0, List.of()
        );
        when(adminService.getStudentsList(any(), any(), any(), any()))
                .thenReturn(List.of(s1));

        mockMvc.perform(get("/api/admin/students?search=Alice")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.data[0].id").value("std-1"))
                .andExpect(jsonPath("$.data[0].name").value("Alice"))
                .andExpect(jsonPath("$.data[0].password").doesNotExist())
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data[0].token").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/admin/students returns 403 when accessed by student")
    void getStudents_studentDenied() throws Exception {
        mockMvc.perform(get("/api/admin/students")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    // 4. GET /api/admin/students/{studentId}
    @Test
    @DisplayName("GET /api/admin/students/{studentId} returns 200 for existing student")
    void getStudentById_existing_returns200() throws Exception {
        AdminStudentDetailDto detail = new AdminStudentDetailDto(
                new AdminStudentDetailDto.ProfileDto("std-1", "u-1", "ST101", "Alice", "alice@college.edu", 2, "CS", new BigDecimal("3.80"), "2026-01-01"),
                new AdminStudentDetailDto.AttendanceDetailDto(new AdminStudentDetailDto.AttendanceOverallDto(30, 28, 93.33, 75, "SAFE", "ON_TRACK", 2, 0), List.of()),
                new AdminStudentDetailDto.AssignmentsDetailDto(2, 1, 0, List.of()),
                new AdminStudentDetailDto.ExamsDetailDto(1, 1, 0, List.of()),
                new AdminStudentDetailDto.QuizzesDetailDto(3, 85.0, List.of()),
                null,
                List.of(),
                new AdminStudentDetailDto.InterviewDetailDto(2, 80.0, List.of(), List.of())
        );
        when(adminService.getStudentDetail("std-1")).thenReturn(detail);

        mockMvc.perform(get("/api/admin/students/std-1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile.name").value("Alice"))
                .andExpect(jsonPath("$.data.profile.password").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/admin/students/{studentId} returns 404 when student not found")
    void getStudentById_notFound_returns404() throws Exception {
        when(adminService.getStudentDetail("std-unknown")).thenReturn(null);

        mockMvc.perform(get("/api/admin/students/std-unknown")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Student with ID 'std-unknown' not found"));
    }

    @Test
    @DisplayName("GET /api/admin/students/{studentId} returns 403 for student role")
    void getStudentById_studentDenied() throws Exception {
        mockMvc.perform(get("/api/admin/students/std-1")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    // 5. GET /api/admin/risks
    @Test
    @DisplayName("GET /api/admin/risks returns 200 for admin and 403 for student")
    void getAcademicRisks_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/risks")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        AdminRisksDataDto risks = new AdminRisksDataDto(
                List.of(new AdminRisksDataDto.AttendanceRiskItem("std-2", "Bob", "CS", 1, 62.0, 20, 12, "CRITICAL")),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
        when(adminService.getAcademicRisks()).thenReturn(risks);

        mockMvc.perform(get("/api/admin/risks")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.attendanceRisk[0].name").value("Bob"));
    }

    // 6. POST /api/admin/insights
    @Test
    @DisplayName("POST /api/admin/insights returns 200 for admin and 403 for student")
    void getAdminInsights_returns200() throws Exception {
        mockMvc.perform(post("/api/admin/insights")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        AdminInsightsDataDto insights = new AdminInsightsDataDto(
                List.of("Observation 1"),
                List.of("Attention 1"),
                List.of("Pattern 1"),
                List.of("Action 1"),
                "Deterministic summary",
                false
        );
        when(adminService.getAdminInsights()).thenReturn(insights);

        mockMvc.perform(post("/api/admin/insights")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.keyObservations[0]").value("Observation 1"))
                .andExpect(jsonPath("$.data.isAiGenerated").value(false));
    }

    // 7. GET /api/admin/documents
    @Test
    @DisplayName("GET /api/admin/documents returns 200 for admin and 403 for student")
    void getAdminDocuments_returns200() throws Exception {
        mockMvc.perform(get("/api/admin/documents")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        AdminDocumentDto doc = new AdminDocumentDto(
                "doc-1", "Handbook", "handbook.pdf", 2048, "u-admin-1", 5, 12, "2026-02-01", "Indexed", "gemini-embedding-2", "Google Gemini"
        );
        when(adminService.getAdminDocuments()).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/admin/documents")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Handbook"))
                .andExpect(jsonPath("$.data[0].embeddingStatus").value("Indexed"));
    }

    // 8. POST /api/admin/subjects
    @Test
    @DisplayName("POST /api/admin/subjects returns 201 for valid body and 400 for invalid body")
    void createSubject_validationAndSuccess() throws Exception {
        // Missing code and name -> 400
        mockMvc.perform(post("/api/admin/subjects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instructor\":\"Dr. Who\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Valid body -> 201
        AdminSubjectDto subjectDto = new AdminSubjectDto("sub-1", "CS301", "Operating Systems", "Dr. Who", 4, "Computer Science");
        when(adminService.createSubject(any(CreateSubjectRequest.class))).thenReturn(subjectDto);

        mockMvc.perform(post("/api/admin/subjects")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CS301\",\"name\":\"Operating Systems\",\"credits\":4}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("CS301"));

        // Student attempt -> 403
        mockMvc.perform(post("/api/admin/subjects")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"CS301\",\"name\":\"Operating Systems\"}"))
                .andExpect(status().isForbidden());
    }

    // 9. POST /api/admin/announcements
    @Test
    @DisplayName("POST /api/admin/announcements returns 201 on success, 400 on empty content, 403 for student")
    void createAnnouncement_validationAndSuccess() throws Exception {
        // Empty content -> 400
        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Midterm Schedule\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Student -> 403
        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Midterm Schedule\",\"content\":\"Exams begin Monday\"}"))
                .andExpect(status().isForbidden());

        // Valid -> 201
        AnnouncementDto anc = new AnnouncementDto("anc-1", "Midterm Schedule", "Exams begin Monday", "u-admin-1", "2026-03-01");
        when(adminService.createAnnouncement(any(CreateAnnouncementRequest.class), any())).thenReturn(anc);

        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Midterm Schedule\",\"content\":\"Exams begin Monday\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Midterm Schedule"));
    }

    // 10. POST /api/admin/faqs
    @Test
    @DisplayName("POST /api/admin/faqs returns 201 on success, 400 on empty fields, 403 for student")
    void createFaq_validationAndSuccess() throws Exception {
        // Missing answer -> 400
        mockMvc.perform(post("/api/admin/faqs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is grading policy?\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // Student -> 403
        mockMvc.perform(post("/api/admin/faqs")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What?\",\"answer\":\"That\"}"))
                .andExpect(status().isForbidden());

        // Valid -> 201
        FaqDto faq = new FaqDto("faq-1", "What is grading policy?", "Relative grading", "Academic", "2026-03-01", "2026-03-01");
        when(adminService.createFaq(any(CreateFaqRequest.class))).thenReturn(faq);

        mockMvc.perform(post("/api/admin/faqs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is grading policy?\",\"answer\":\"Relative grading\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.question").value("What is grading policy?"));
    }

    // 11. GET /api/admin/faqs
    @Test
    @DisplayName("GET /api/admin/faqs returns 200 for admin, 403 for student")
    void getFaqs_authorizationAndSuccess() throws Exception {
        // Student -> 403
        mockMvc.perform(get("/api/admin/faqs")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Admin -> 200
        FaqDto faq = new FaqDto("faq-1", "What is grading policy?", "Relative grading", "Academic", "2026-03-01", "2026-03-01");
        when(adminService.getFaqsList()).thenReturn(List.of(faq));

        mockMvc.perform(get("/api/admin/faqs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("faq-1"));
    }

    // 12. PUT /api/admin/faqs/{id}
    @Test
    @DisplayName("PUT /api/admin/faqs/{id} returns 200 on update, 400 on missing body, 403 for student")
    void updateFaq_authorizationAndSuccess() throws Exception {
        // Student -> 403
        mockMvc.perform(put("/api/admin/faqs/faq-1")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Updated?\",\"answer\":\"Yes\"}"))
                .andExpect(status().isForbidden());

        // Missing field -> 400
        mockMvc.perform(put("/api/admin/faqs/faq-1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest());

        // Admin -> 200
        FaqDto updated = new FaqDto("faq-1", "Updated question?", "Updated answer", "Academic", "2026-03-01", "2026-03-02");
        when(adminService.updateFaq(eq("faq-1"), any(CreateFaqRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/admin/faqs/faq-1")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"Updated question?\",\"answer\":\"Updated answer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").value("Updated answer"));
    }

    // 13. DELETE /api/admin/faqs/{id}
    @Test
    @DisplayName("DELETE /api/admin/faqs/{id} returns 200 for admin, 403 for student")
    void deleteFaq_authorizationAndSuccess() throws Exception {
        // Student -> 403
        mockMvc.perform(delete("/api/admin/faqs/faq-1")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Admin -> 200
        doNothing().when(adminService).deleteFaq("faq-1");

        mockMvc.perform(delete("/api/admin/faqs/faq-1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
