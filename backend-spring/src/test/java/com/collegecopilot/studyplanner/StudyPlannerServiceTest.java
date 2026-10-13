package com.collegecopilot.studyplanner;

import com.collegecopilot.auth.Role;
import com.collegecopilot.auth.User;
import com.collegecopilot.auth.UserRepository;
import com.collegecopilot.student.*;
import com.collegecopilot.studyplanner.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudyPlannerServiceTest {

    @Mock
    private StudyPlanRepository studyPlanRepository;

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private GeminiStudyPlannerService geminiService;

    @InjectMocks
    private StudyPlannerService studyPlannerService;

    private Student student1;
    private Student student2;
    private User user1;
    private Subject sub1;
    private Subject sub2;
    private StudyPlan plan1;
    private StudySession session1;
    private StudySession session2;

    @BeforeEach
    void setUp() {
        student1 = new Student("std-1", "user-1", "2026-CS-001", 4, "Computer Science", new BigDecimal("3.85"));
        student2 = new Student("std-2", "user-2", "2026-CS-002", 4, "Computer Science", new BigDecimal("3.50"));
        user1 = new User("user-1", "Alex Mercer", "alex@student.edu", "hashed", Role.STUDENT);

        sub1 = new Subject("sub-1", "CS101", "Data Structures & Algorithms", "Dr. Smith", 4, "Computer Science");
        sub2 = new Subject("sub-2", "CS102", "Database Systems", "Dr. Jones", 3, "Computer Science");

        plan1 = new StudyPlan("sp-1", "std-1", "Midterm Sprint Plan 2026", LocalDate.now(), LocalDate.now().plusDays(7), "Active");
        plan1.setTotalHours(new BigDecimal("10.0"));

        session1 = new StudySession("ss-1", "sp-1", "sub-1", "Binary Search Trees", LocalDate.now(), LocalTime.of(16, 0), LocalTime.of(18, 0));
        session1.setPriority("High");
        session1.setStatus("Pending");

        session2 = new StudySession("ss-2", "sp-1", "sub-2", "SQL Joins", LocalDate.now().plusDays(1), LocalTime.of(19, 0), LocalTime.of(21, 0));
        session2.setPriority("Medium");
        session2.setStatus("Completed");
    }

    @Test
    @DisplayName("getStudyPlan returns active plan and sessions for authenticated student")
    void getStudyPlan_whenActivePlanExists_returnsPlanAndSessions() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(studyPlanRepository.findFirstByStudentIdAndStatus("std-1", "Active")).thenReturn(Optional.of(plan1));
        when(studySessionRepository.findByPlanIdOrderBySessionDateAscStartTimeAsc("sp-1")).thenReturn(List.of(session1, session2));
        when(subjectRepository.findAll()).thenReturn(List.of(sub1, sub2));

        PlannerGetResponse response = studyPlannerService.getStudyPlan("std-1");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getPlan());
        assertEquals("sp-1", response.getData().getPlan().getId());
        assertEquals("Midterm Sprint Plan 2026", response.getData().getPlan().getTitle());
        assertEquals(2, response.getData().getSessions().size());
        assertEquals("Binary Search Trees", response.getData().getSessions().get(0).getTopic());
        assertEquals("CS101", response.getData().getSessions().get(0).getSubjectCode());
    }

    @Test
    @DisplayName("getStudyPlan returns empty data when student has no active plan")
    void getStudyPlan_whenNoActivePlan_returnsEmptyData() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(studyPlanRepository.findFirstByStudentIdAndStatus("std-1", "Active")).thenReturn(Optional.empty());

        PlannerGetResponse response = studyPlannerService.getStudyPlan("std-1");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNull(response.getData().getPlan());
        assertTrue(response.getData().getSessions().isEmpty());
    }

    @Test
    @DisplayName("getStudyPlan returns empty data when student is not found")
    void getStudyPlan_whenStudentNotFound_returnsEmptyData() {
        when(studentRepository.findById("unknown")).thenReturn(Optional.empty());
        when(studentRepository.findByUserId("unknown")).thenReturn(Optional.empty());

        PlannerGetResponse response = studyPlannerService.getStudyPlan("unknown");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNull(response.getData().getPlan());
        assertTrue(response.getData().getSessions().isEmpty());
    }

    @Test
    @DisplayName("generateStudyPlan gathers context, calls Gemini service, and saves new plan & sessions")
    void generateStudyPlan_success_createsPlanAndSessions() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user1));
        when(subjectRepository.findAll()).thenReturn(List.of(sub1, sub2));
        when(attendanceRepository.findByStudentId("std-1")).thenReturn(List.of());
        when(assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc("std-1")).thenReturn(List.of());
        when(examRepository.findByStudentIdOrLegacyOrderByExamDateAsc("std-1")).thenReturn(List.of());
        when(studyPlanRepository.findByStudentId("std-1")).thenReturn(new ArrayList<>(List.of(plan1)));

        StructuredPlanDto mockAiPlan = new StructuredPlanDto(
                "Personalized 7-day study schedule",
                List.of(new PlanPriorityDto("Data Structures & Algorithms", "Midterm exam", "High")),
                List.of(new DailyPlanDto(LocalDate.now().toString(), List.of(
                        new DailyTaskDto("16:00", "18:00", "Data Structures & Algorithms", "BST practice", "Exam Prep", "High")
                ))),
                List.of("Use active recall")
        );
        when(geminiService.generateStructuredPlan(any(), any())).thenReturn(mockAiPlan);

        PlannerGenerateRequest request = new PlannerGenerateRequest("Sprint 2026", 4.0, "16:00", "20:00", List.of("Mon", "Tue"), "CS101");

        PlannerGenerateResponse response = studyPlannerService.generateStudyPlan("std-1", request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("AI Personalized Study Plan generated successfully", response.getMessage());
        assertNotNull(response.getData().getPlan());
        assertEquals("Sprint 2026", response.getData().getPlan().getTitle());
        assertEquals(1, response.getData().getSessions().size());
        assertEquals("BST practice", response.getData().getSessions().get(0).getTopic());

        // Verify previous plan was marked Completed and new plan was saved
        assertEquals("Completed", plan1.getStatus());
        verify(studyPlanRepository, atLeastOnce()).save(any(StudyPlan.class));
        verify(studySessionRepository, atLeastOnce()).save(any(StudySession.class));
    }

    @Test
    @DisplayName("generateStudyPlan avoids scheduling tasks in the past by clamping past dates to current date")
    void generateStudyPlan_whenAiReturnsPastDate_clampsToCurrentDate() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user1));
        when(subjectRepository.findAll()).thenReturn(List.of(sub1, sub2));
        when(attendanceRepository.findByStudentId("std-1")).thenReturn(List.of());
        when(assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc("std-1")).thenReturn(List.of());
        when(examRepository.findByStudentIdOrLegacyOrderByExamDateAsc("std-1")).thenReturn(List.of());
        when(studyPlanRepository.findByStudentId("std-1")).thenReturn(new ArrayList<>(List.of(plan1)));

        // AI generated a plan with a past date (yesterday)
        LocalDate pastDate = LocalDate.now().minusDays(5);
        StructuredPlanDto mockAiPlanWithPastDate = new StructuredPlanDto(
                "Plan with past date",
                List.of(new PlanPriorityDto("Data Structures & Algorithms", "Past task", "High")),
                List.of(new DailyPlanDto(pastDate.toString(), List.of(
                        new DailyTaskDto("16:00", "18:00", "Data Structures & Algorithms", "Reviewing previous material", "Past Task", "High")
                ))),
                List.of("Review notes")
        );
        when(geminiService.generateStructuredPlan(any(), any())).thenReturn(mockAiPlanWithPastDate);

        PlannerGenerateRequest request = new PlannerGenerateRequest();
        PlannerGenerateResponse response = studyPlannerService.generateStudyPlan("std-1", request);

        assertNotNull(response);
        assertEquals(1, response.getData().getSessions().size());
        // Verify sessionDate was clamped to today and not in the past
        LocalDate savedDate = LocalDate.parse(response.getData().getSessions().get(0).getSessionDate());
        assertFalse(savedDate.isBefore(LocalDate.now()));
        assertEquals(LocalDate.now().toString(), response.getData().getSessions().get(0).getSessionDate());
    }

    @Test
    @DisplayName("generateStudyPlan throws 403 when student is not found")
    void generateStudyPlan_whenStudentNotFound_throwsForbidden() {
        when(studentRepository.findById("unknown")).thenReturn(Optional.empty());
        when(studentRepository.findByUserId("unknown")).thenReturn(Optional.empty());

        PlannerGenerateRequest request = new PlannerGenerateRequest();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studyPlannerService.generateStudyPlan("unknown", request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Only students can generate study plans"));
    }

    @Test
    @DisplayName("updateSessionStatus updates valid status when owned by student")
    void updateSessionStatus_success() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(studySessionRepository.findById("ss-1")).thenReturn(Optional.of(session1));
        when(studyPlanRepository.findById("sp-1")).thenReturn(Optional.of(plan1));
        when(subjectRepository.findAll()).thenReturn(List.of(sub1, sub2));

        SessionStatusUpdateRequest request = new SessionStatusUpdateRequest("Completed");
        SessionUpdateResponse response = studyPlannerService.updateSessionStatus("std-1", "ss-1", request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Completed", response.getData().getStatus());
        assertEquals("Completed", session1.getStatus());
        verify(studySessionRepository).save(session1);
    }

    @Test
    @DisplayName("updateSessionStatus rejects cross-student modification with 403")
    void updateSessionStatus_crossStudentOwnership_throwsForbidden() {
        // student2 attempts to update session1 (which belongs to student1's plan)
        when(studentRepository.findById("std-2")).thenReturn(Optional.of(student2));
        when(studySessionRepository.findById("ss-1")).thenReturn(Optional.of(session1));
        when(studyPlanRepository.findById("sp-1")).thenReturn(Optional.of(plan1)); // plan1 has student_id = std-1

        SessionStatusUpdateRequest request = new SessionStatusUpdateRequest("Completed");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studyPlannerService.updateSessionStatus("std-2", "ss-1", request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You do not own this study session"));
    }

    @Test
    @DisplayName("updateSessionStatus throws 404 when session does not exist")
    void updateSessionStatus_sessionNotFound_throwsNotFound() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(studySessionRepository.findById("ss-unknown")).thenReturn(Optional.empty());

        SessionStatusUpdateRequest request = new SessionStatusUpdateRequest("Completed");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studyPlannerService.updateSessionStatus("std-1", "ss-unknown", request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Session not found"));
    }

    @Test
    @DisplayName("updateSessionStatus rejects invalid status with 400")
    void updateSessionStatus_invalidStatus_throwsBadRequest() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(studySessionRepository.findById("ss-1")).thenReturn(Optional.of(session1));
        when(studyPlanRepository.findById("sp-1")).thenReturn(Optional.of(plan1));

        SessionStatusUpdateRequest request = new SessionStatusUpdateRequest("Archived");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studyPlannerService.updateSessionStatus("std-1", "ss-1", request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid status 'Archived'"));
    }

    @Test
    @DisplayName("replanSchedule shifts non-completed sessions and preserves completed sessions")
    void replanSchedule_success_shiftsOnlyPendingSessions() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(studyPlanRepository.findFirstByStudentIdAndStatus("std-1", "Active")).thenReturn(Optional.of(plan1));
        when(studySessionRepository.findByPlanIdOrderBySessionDateAscStartTimeAsc("sp-1")).thenReturn(List.of(session1, session2));
        when(subjectRepository.findAll()).thenReturn(List.of(sub1, sub2));

        LocalDate oldDate = session1.getSessionDate();

        PlannerReplanResponse response = studyPlannerService.replanSchedule("std-1");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertTrue(response.getMessage().contains("Replanned remaining sessions successfully"));
        assertEquals(2, response.getData().size());

        // session1 (Pending) was shifted forward
        assertEquals(LocalDate.now().plusDays(1), session1.getSessionDate());
        assertEquals("Pending", session1.getStatus());
        verify(studySessionRepository).save(session1);

        // session2 (Completed) was preserved
        assertEquals("Completed", session2.getStatus());
        verify(studySessionRepository, never()).save(session2);
    }

    @Test
    @DisplayName("replanSchedule throws 404 when no active plan exists")
    void replanSchedule_noActivePlan_throwsNotFound() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(studyPlanRepository.findFirstByStudentIdAndStatus("std-1", "Active")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                studyPlannerService.replanSchedule("std-1"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("No active study plan to replan"));
    }
}
