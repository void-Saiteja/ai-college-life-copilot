package com.collegecopilot.assignment;

import com.collegecopilot.assignment.dto.*;
import com.collegecopilot.student.*;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private GeminiAssignmentAdvisorService advisorService;

    @InjectMocks
    private AssignmentService assignmentService;

    private Student student1;
    private Student student2;
    private Subject csSubject;
    private Assignment assignment1;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.of(2026, 10, 7);

        student1 = new Student("std-1", "user-1", "std-001", 4, "Computer Science", new BigDecimal("3.80"));
        student2 = new Student("std-2", "user-2", "std-002", 4, "Information Tech", new BigDecimal("3.80"));

        csSubject = new Subject("sub-1", "CS201", "Data Structures", "Dr. Turing", 4, "Computer Science");

        assignment1 = new Assignment("asg-1", "std-1", "sub-1", "Implement AVL Tree", today.plusDays(2), "High", "Pending");
    }

    @Test
    @DisplayName("calculateAssignmentMetrics: COMPLETED status returns 0 days remaining, COMPLETED deadlineStatus and urgency")
    void testMetricsCompleted() {
        Assignment asg = new Assignment("asg-c", "std-1", "sub-1", "Done task", today.plusDays(5), "Low", "Completed");
        AssignmentMetrics metrics = assignmentService.calculateAssignmentMetrics(asg, today);

        assertEquals(0, metrics.getDaysRemaining());
        assertEquals("COMPLETED", metrics.getDeadlineStatus());
        assertEquals("COMPLETED", metrics.getUrgency());
    }

    @Test
    @DisplayName("calculateAssignmentMetrics: null dueDate returns 999 days, UPCOMING, LOW urgency")
    void testMetricsNullDueDate() {
        Assignment asg = new Assignment("asg-n", "std-1", "sub-1", "No date", null, "Low", "Pending");
        AssignmentMetrics metrics = assignmentService.calculateAssignmentMetrics(asg, today);

        assertEquals(999, metrics.getDaysRemaining());
        assertEquals("UPCOMING", metrics.getDeadlineStatus());
        assertEquals("LOW", metrics.getUrgency());
    }

    @Test
    @DisplayName("calculateAssignmentMetrics: past dueDate returns OVERDUE and CRITICAL urgency")
    void testMetricsOverdue() {
        Assignment asg = new Assignment("asg-o", "std-1", "sub-1", "Late task", today.minusDays(2), "High", "Pending");
        AssignmentMetrics metrics = assignmentService.calculateAssignmentMetrics(asg, today);

        assertEquals(-2, metrics.getDaysRemaining());
        assertEquals("OVERDUE", metrics.getDeadlineStatus());
        assertEquals("CRITICAL", metrics.getUrgency());
    }

    @Test
    @DisplayName("calculateAssignmentMetrics: dueDate today returns DUE_TODAY and CRITICAL urgency")
    void testMetricsDueToday() {
        Assignment asg = new Assignment("asg-t", "std-1", "sub-1", "Today task", today, "High", "Pending");
        AssignmentMetrics metrics = assignmentService.calculateAssignmentMetrics(asg, today);

        assertEquals(0, metrics.getDaysRemaining());
        assertEquals("DUE_TODAY", metrics.getDeadlineStatus());
        assertEquals("CRITICAL", metrics.getUrgency());
    }

    @Test
    @DisplayName("calculateAssignmentMetrics: 1-3 days remaining returns DUE_SOON and HIGH urgency")
    void testMetricsDueSoon() {
        Assignment asg = new Assignment("asg-s", "std-1", "sub-1", "Soon task", today.plusDays(3), "Medium", "Pending");
        AssignmentMetrics metrics = assignmentService.calculateAssignmentMetrics(asg, today);

        assertEquals(3, metrics.getDaysRemaining());
        assertEquals("DUE_SOON", metrics.getDeadlineStatus());
        assertEquals("HIGH", metrics.getUrgency());
    }

    @Test
    @DisplayName("calculateAssignmentMetrics: 4-7 days remaining returns UPCOMING and MEDIUM urgency")
    void testMetricsUpcomingMedium() {
        Assignment asg = new Assignment("asg-m", "std-1", "sub-1", "Medium task", today.plusDays(6), "Medium", "Pending");
        AssignmentMetrics metrics = assignmentService.calculateAssignmentMetrics(asg, today);

        assertEquals(6, metrics.getDaysRemaining());
        assertEquals("UPCOMING", metrics.getDeadlineStatus());
        assertEquals("MEDIUM", metrics.getUrgency());
    }

    @Test
    @DisplayName("calculateAssignmentMetrics: >7 days remaining returns UPCOMING and LOW urgency")
    void testMetricsUpcomingLow() {
        Assignment asg = new Assignment("asg-l", "std-1", "sub-1", "Later task", today.plusDays(14), "Low", "Pending");
        AssignmentMetrics metrics = assignmentService.calculateAssignmentMetrics(asg, today);

        assertEquals(14, metrics.getDaysRemaining());
        assertEquals("UPCOMING", metrics.getDeadlineStatus());
        assertEquals("LOW", metrics.getUrgency());
    }

    @Test
    @DisplayName("getAssignments: returns formatted list and summaries for authenticated student")
    void testGetAssignmentsSuccess() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc("std-1")).thenReturn(List.of(assignment1));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(csSubject));

        AssignmentListResponse response = assignmentService.getAssignments("user-1", "all");

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("Implement AVL Tree", response.getData().get(0).getTitle());
        assertEquals("CS201", response.getData().get(0).getCourse());
        assertEquals(1, response.getSummary().getTotal());
        assertEquals(1, response.getSummary().getPending());
        assertEquals(0, response.getSummary().getCompleted());
    }

    @Test
    @DisplayName("getAssignmentById: returns assignment if owned by student")
    void testGetAssignmentByIdSuccess() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(assignmentRepository.findById("asg-1")).thenReturn(Optional.of(assignment1));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(csSubject));

        AssignmentDto dto = assignmentService.getAssignmentById("user-1", "asg-1");

        assertNotNull(dto);
        assertEquals("asg-1", dto.getId());
        assertEquals("Implement AVL Tree", dto.getTitle());
    }

    @Test
    @DisplayName("getAssignmentById: throws 403 when trying to access another student's assignment")
    void testGetAssignmentByIdForbidden() {
        when(studentRepository.findByUserId("user-2")).thenReturn(Optional.of(student2));
        when(assignmentRepository.findById("asg-1")).thenReturn(Optional.of(assignment1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> assignmentService.getAssignmentById("user-2", "asg-1"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You do not own this assignment"));
    }

    @Test
    @DisplayName("getAssignmentById: throws 404 when assignment not found")
    void testGetAssignmentByIdNotFound() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(assignmentRepository.findById("asg-missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> assignmentService.getAssignmentById("user-1", "asg-missing"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Assignment not found"));
    }

    @Test
    @DisplayName("createAssignment: creates and saves new assignment with studentId")
    void testCreateAssignmentSuccess() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(csSubject));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignmentCreateRequest req = new AssignmentCreateRequest(
                "Build B-Tree", "CS201", "sub-1", "2026-10-15", "High", 3.5, "Assignment", "Detailed specs"
        );

        AssignmentDto created = assignmentService.createAssignment("user-1", req);

        assertNotNull(created);
        assertEquals("Build B-Tree", created.getTitle());
        assertEquals("std-1", created.getStudentId());
        assertEquals("sub-1", created.getSubjectId());
        assertEquals(3.5, created.getEstimatedHours());
        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    @DisplayName("createAssignment: throws 400 when title is missing")
    void testCreateAssignmentMissingTitle() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));

        AssignmentCreateRequest req = new AssignmentCreateRequest();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> assignmentService.createAssignment("user-1", req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Title is required"));
    }

    @Test
    @DisplayName("updateAssignment: updates record when student is owner")
    void testUpdateAssignmentSuccess() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(assignmentRepository.findById("asg-1")).thenReturn(Optional.of(assignment1));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(csSubject));

        AssignmentUpdateRequest req = new AssignmentUpdateRequest(
                "Updated AVL Tree", "New desc", "CS201", "sub-1", "2026-10-20", "Low", "In Progress", 4.0, "Homework"
        );

        AssignmentDto updated = assignmentService.updateAssignment("user-1", "asg-1", req);

        assertEquals("Updated AVL Tree", updated.getTitle());
        assertEquals("In Progress", updated.getStatus());
        verify(assignmentRepository).save(assignment1);
    }

    @Test
    @DisplayName("updateAssignment: throws 403 on cross-student update")
    void testUpdateAssignmentForbidden() {
        when(studentRepository.findByUserId("user-2")).thenReturn(Optional.of(student2));
        when(assignmentRepository.findById("asg-1")).thenReturn(Optional.of(assignment1));

        AssignmentUpdateRequest req = new AssignmentUpdateRequest();
        req.setTitle("Hacked title");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> assignmentService.updateAssignment("user-2", "asg-1", req));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteAssignment: deletes record when student is owner")
    void testDeleteAssignmentSuccess() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(assignmentRepository.findById("asg-1")).thenReturn(Optional.of(assignment1));

        AssignmentDeleteResponse res = assignmentService.deleteAssignment("user-1", "asg-1");

        assertTrue(res.isSuccess());
        assertEquals("Assignment deleted successfully", res.getMessage());
        verify(assignmentRepository).delete(assignment1);
    }

    @Test
    @DisplayName("deleteAssignment: throws 403 on cross-student delete")
    void testDeleteAssignmentForbidden() {
        when(studentRepository.findByUserId("user-2")).thenReturn(Optional.of(student2));
        when(assignmentRepository.findById("asg-1")).thenReturn(Optional.of(assignment1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> assignmentService.deleteAssignment("user-2", "asg-1"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(assignmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("runAssignmentAdvisor: calls advisorService with formatted assignments")
    void testRunAssignmentAdvisor() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc("std-1")).thenReturn(List.of(assignment1));
        when(examRepository.findByStudentIdOrderByExamDateAsc("std-1")).thenReturn(List.of());
        when(attendanceRepository.findByStudentId("std-1")).thenReturn(List.of());

        AdvisorDataDto mockAdvisor = new AdvisorDataDto("Analysis complete", "Focus on AVL Tree", List.of(), false);
        when(advisorService.generatePrioritization(anyList(), anyList(), anyList())).thenReturn(mockAdvisor);

        AdvisorDataDto res = assignmentService.runAssignmentAdvisor("user-1");

        assertNotNull(res);
        assertEquals("Analysis complete", res.getSummary());
        assertEquals("Focus on AVL Tree", res.getRecommendation());
    }
}
