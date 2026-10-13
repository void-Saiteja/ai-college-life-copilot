package com.collegecopilot.exam;

import com.collegecopilot.exam.dto.*;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private GeminiExamAdvisorService advisorService;

    @InjectMocks
    private ExamService examService;

    private Student student1;
    private Student student2;
    private Subject csSubject;
    private Exam exam1;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();

        student1 = new Student("std-1", "user-1", "std-001", 4, "Computer Science", new BigDecimal("3.80"));
        student2 = new Student("std-2", "user-2", "std-002", 4, "Information Tech", new BigDecimal("3.80"));

        csSubject = new Subject("sub-1", "CS201", "Data Structures", "Dr. Turing", 4, "Computer Science");

        exam1 = new Exam("exm-1", "std-1", "sub-1", "Midterm Exam", today.plusDays(2), LocalTime.of(10, 0), "Hall A");
    }

    @Test
    @DisplayName("calculateExamMetrics: COMPLETED status returns 0 days remaining, COMPLETED examStatus and urgency")
    void testMetricsCompleted() {
        Exam exam = new Exam("exm-c", "std-1", "sub-1", "Done exam", today.plusDays(5), LocalTime.of(9, 0), "Hall A");
        exam.setStatus("Completed");
        ExamMetrics metrics = examService.calculateExamMetrics(exam, today);

        assertEquals(0, metrics.getDaysRemaining());
        assertEquals("COMPLETED", metrics.getExamStatus());
        assertEquals("COMPLETED", metrics.getUrgency());
    }

    @Test
    @DisplayName("calculateExamMetrics: null examDate returns 999 days, UPCOMING, LOW urgency")
    void testMetricsNullDate() {
        Exam exam = new Exam("exm-n", "std-1", "sub-1", "No date", null, LocalTime.of(9, 0), "Hall A");
        ExamMetrics metrics = examService.calculateExamMetrics(exam, today);

        assertEquals(999, metrics.getDaysRemaining());
        assertEquals("UPCOMING", metrics.getExamStatus());
        assertEquals("LOW", metrics.getUrgency());
    }

    @Test
    @DisplayName("calculateExamMetrics: past date returns COMPLETED status and urgency")
    void testMetricsPastDate() {
        Exam exam = new Exam("exm-p", "std-1", "sub-1", "Past exam", today.minusDays(3), LocalTime.of(9, 0), "Hall A");
        ExamMetrics metrics = examService.calculateExamMetrics(exam, today);

        assertEquals(-3, metrics.getDaysRemaining());
        assertEquals("COMPLETED", metrics.getExamStatus());
        assertEquals("COMPLETED", metrics.getUrgency());
    }

    @Test
    @DisplayName("calculateExamMetrics: exam today returns TODAY and CRITICAL urgency")
    void testMetricsToday() {
        Exam exam = new Exam("exm-t", "std-1", "sub-1", "Today exam", today, LocalTime.of(9, 0), "Hall A");
        ExamMetrics metrics = examService.calculateExamMetrics(exam, today);

        assertEquals(0, metrics.getDaysRemaining());
        assertEquals("TODAY", metrics.getExamStatus());
        assertEquals("CRITICAL", metrics.getUrgency());
    }

    @Test
    @DisplayName("calculateExamMetrics: exam in 1-3 days returns VERY_SOON and HIGH urgency")
    void testMetricsVerySoon() {
        Exam exam = new Exam("exm-vs", "std-1", "sub-1", "Soon exam", today.plusDays(2), LocalTime.of(9, 0), "Hall A");
        ExamMetrics metrics = examService.calculateExamMetrics(exam, today);

        assertEquals(2, metrics.getDaysRemaining());
        assertEquals("VERY_SOON", metrics.getExamStatus());
        assertEquals("HIGH", metrics.getUrgency());
    }

    @Test
    @DisplayName("calculateExamMetrics: exam in 4-7 days returns UPCOMING and MEDIUM urgency")
    void testMetricsMedium() {
        Exam exam = new Exam("exm-m", "std-1", "sub-1", "Medium exam", today.plusDays(5), LocalTime.of(9, 0), "Hall A");
        ExamMetrics metrics = examService.calculateExamMetrics(exam, today);

        assertEquals(5, metrics.getDaysRemaining());
        assertEquals("UPCOMING", metrics.getExamStatus());
        assertEquals("MEDIUM", metrics.getUrgency());
    }

    @Test
    @DisplayName("calculateExamMetrics: exam in >7 days returns UPCOMING and LOW urgency")
    void testMetricsLow() {
        Exam exam = new Exam("exm-l", "std-1", "sub-1", "Later exam", today.plusDays(15), LocalTime.of(9, 0), "Hall A");
        ExamMetrics metrics = examService.calculateExamMetrics(exam, today);

        assertEquals(15, metrics.getDaysRemaining());
        assertEquals("UPCOMING", metrics.getExamStatus());
        assertEquals("LOW", metrics.getUrgency());
    }

    @Test
    @DisplayName("getExams: returns formatted exams and summary for authenticated student")
    void testGetExamsSuccess() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(examRepository.findByStudentIdOrLegacyOrderByExamDateAsc("std-1")).thenReturn(List.of(exam1));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(csSubject));

        ExamListResponse response = examService.getExams("user-1", "all");

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().size());
        assertEquals("Midterm Exam", response.getData().get(0).getTitle());
        assertEquals("CS201", response.getData().get(0).getSubjectCode());
        assertEquals(1, response.getSummary().getTotal());
        assertEquals(1, response.getSummary().getUpcoming());
    }

    @Test
    @DisplayName("getExamById: returns exam if owned by student")
    void testGetExamByIdSuccess() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(examRepository.findById("exm-1")).thenReturn(Optional.of(exam1));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(csSubject));

        ExamDto dto = examService.getExamById("user-1", "exm-1");

        assertNotNull(dto);
        assertEquals("exm-1", dto.getId());
        assertEquals("Midterm Exam", dto.getTitle());
    }

    @Test
    @DisplayName("getExamById: throws 403 when trying to access another student's exam")
    void testGetExamByIdForbidden() {
        when(studentRepository.findByUserId("user-2")).thenReturn(Optional.of(student2));
        when(examRepository.findById("exm-1")).thenReturn(Optional.of(exam1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> examService.getExamById("user-2", "exm-1"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You do not own this exam record"));
    }

    @Test
    @DisplayName("getExamById: throws 404 when exam not found")
    void testGetExamByIdNotFound() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(examRepository.findById("exm-missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> examService.getExamById("user-1", "exm-missing"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Exam not found"));
    }

    @Test
    @DisplayName("createExam: creates and saves new exam with studentId")
    void testCreateExamSuccess() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(csSubject));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));

        ExamCreateRequest req = new ExamCreateRequest(
                "Final Exam", "2026-10-25", "sub-1", "02:00 PM", "Auditorium B", "Modules 1-5", "Upcoming"
        );

        ExamDto created = examService.createExam("user-1", req);

        assertNotNull(created);
        assertEquals("Final Exam", created.getTitle());
        assertEquals("std-1", created.getStudentId());
        assertEquals("sub-1", created.getSubjectId());
        verify(examRepository).save(any(Exam.class));
    }

    @Test
    @DisplayName("createExam: throws 400 when title or exam_date missing")
    void testCreateExamMissingFields() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));

        ExamCreateRequest req = new ExamCreateRequest();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> examService.createExam("user-1", req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Title and exam date are required"));
    }

    @Test
    @DisplayName("updateExam: updates record when student is owner")
    void testUpdateExamSuccess() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(examRepository.findById("exm-1")).thenReturn(Optional.of(exam1));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(csSubject));

        ExamUpdateRequest req = new ExamUpdateRequest(
                "Updated Midterm", "2026-10-18", "sub-1", "11:00 AM", "Room 101", "Revised syllabus", "Upcoming"
        );

        ExamDto updated = examService.updateExam("user-1", "exm-1", req);

        assertEquals("Updated Midterm", updated.getTitle());
        assertEquals("Room 101", updated.getLocation());
        verify(examRepository).save(exam1);
    }

    @Test
    @DisplayName("updateExam: throws 403 on cross-student update")
    void testUpdateExamForbidden() {
        when(studentRepository.findByUserId("user-2")).thenReturn(Optional.of(student2));
        when(examRepository.findById("exm-1")).thenReturn(Optional.of(exam1));

        ExamUpdateRequest req = new ExamUpdateRequest();
        req.setTitle("Hacked exam");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> examService.updateExam("user-2", "exm-1", req));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(examRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteExam: deletes record when student is owner")
    void testDeleteExamSuccess() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(examRepository.findById("exm-1")).thenReturn(Optional.of(exam1));

        ExamDeleteResponse res = examService.deleteExam("user-1", "exm-1");

        assertTrue(res.isSuccess());
        assertEquals("Exam deleted successfully", res.getMessage());
        verify(examRepository).delete(exam1);
    }

    @Test
    @DisplayName("deleteExam: throws 403 on cross-student delete")
    void testDeleteExamForbidden() {
        when(studentRepository.findByUserId("user-2")).thenReturn(Optional.of(student2));
        when(examRepository.findById("exm-1")).thenReturn(Optional.of(exam1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> examService.deleteExam("user-2", "exm-1"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(examRepository, never()).delete(any());
    }

    @Test
    @DisplayName("runExamAdvisor: calls advisorService with formatted exams")
    void testRunExamAdvisor() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(examRepository.findByStudentIdOrLegacyOrderByExamDateAsc("std-1")).thenReturn(List.of(exam1));
        when(assignmentRepository.findByStudentId("std-1")).thenReturn(List.of());
        when(attendanceRepository.findByStudentId("std-1")).thenReturn(List.of());

        ExamAdvisorDataDto mockAdvisor = new ExamAdvisorDataDto("Exam Strategy", "Review Data Structures", List.of(), false);
        when(advisorService.generatePrioritization(anyList(), anyList(), anyList())).thenReturn(mockAdvisor);

        ExamAdvisorDataDto res = examService.runExamAdvisor("user-1");

        assertNotNull(res);
        assertEquals("Exam Strategy", res.getSummary());
        assertEquals("Review Data Structures", res.getRecommendation());
    }
}
