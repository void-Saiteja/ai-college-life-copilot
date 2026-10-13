package com.collegecopilot.attendance;

import com.collegecopilot.attendance.dto.*;
import com.collegecopilot.student.Attendance;
import com.collegecopilot.student.AttendanceRepository;
import com.collegecopilot.student.Student;
import com.collegecopilot.student.StudentRepository;
import com.collegecopilot.student.Subject;
import com.collegecopilot.student.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private AttendanceService attendanceService;

    private Student student1;
    private Student student2;
    private Subject mathSubject;
    private Attendance attendance1;

    @BeforeEach
    void setUp() {
        student1 = new Student("std-1", "user-1", "std-001", 4, "Computer Science", new java.math.BigDecimal("3.80"));
        student2 = new Student("std-2", "user-2", "std-002", 4, "Information Tech", new java.math.BigDecimal("3.80"));

        mathSubject = new Subject("sub-1", "CS101", "Calculus", "Dr. Ramanujan", 4, "Mathematics");
        attendance1 = new Attendance("att-1", "std-1", "sub-1", 40, 36);
    }

    @Test
    @DisplayName("calculateAttendanceMetrics: NO_CLASSES when conducted = 0")
    void testNoClassesConducted() {
        AttendanceMetrics metrics = attendanceService.calculateAttendanceMetrics(0, 0, 75);

        assertEquals(0, metrics.getConducted());
        assertEquals(0, metrics.getAttended());
        assertEquals(0.00, metrics.getPercentage());
        assertEquals(75, metrics.getTargetPercentage());
        assertEquals(0, metrics.getMaxCanMiss());
        assertEquals(0, metrics.getMinRequiredToAttend());
        assertEquals("No Classes Conducted", metrics.getStatus());
        assertEquals("NO_CLASSES", metrics.getRiskClassification());
    }

    @Test
    @DisplayName("calculateAttendanceMetrics: SAFE status when percentage >= target + 5")
    void testSafeStatus() {
        // 36 attended out of 40 = 90.00% >= 80% (75 + 5)
        AttendanceMetrics metrics = attendanceService.calculateAttendanceMetrics(40, 36, 75);

        assertEquals(40, metrics.getConducted());
        assertEquals(36, metrics.getAttended());
        assertEquals(90.00, metrics.getPercentage());
        assertEquals(75, metrics.getTargetPercentage());
        assertEquals("SAFE", metrics.getRiskClassification());
        assertEquals("Safe / Good (>=80%)", metrics.getStatus());
        assertEquals(8, metrics.getMaxCanMiss());
        assertEquals(0, metrics.getMinRequiredToAttend());
    }

    @Test
    @DisplayName("calculateAttendanceMetrics: WARNING status when target <= percentage < target + 5")
    void testWarningStatus() {
        // 31 attended out of 40 = 77.50% (between 75% and 80%)
        AttendanceMetrics metrics = attendanceService.calculateAttendanceMetrics(40, 31, 75);

        assertEquals(40, metrics.getConducted());
        assertEquals(31, metrics.getAttended());
        assertEquals(77.50, metrics.getPercentage());
        assertEquals(75, metrics.getTargetPercentage());
        assertEquals("WARNING", metrics.getRiskClassification());
        assertEquals("At Risk / Warning (75-80%)", metrics.getStatus());
        assertEquals(1, metrics.getMaxCanMiss());
        assertEquals(0, metrics.getMinRequiredToAttend());
    }

    @Test
    @DisplayName("calculateAttendanceMetrics: CRITICAL status when percentage < target")
    void testCriticalStatus() {
        // 26 attended out of 40 = 65.00% (< 75%)
        AttendanceMetrics metrics = attendanceService.calculateAttendanceMetrics(40, 26, 75);

        assertEquals(40, metrics.getConducted());
        assertEquals(26, metrics.getAttended());
        assertEquals(65.00, metrics.getPercentage());
        assertEquals(75, metrics.getTargetPercentage());
        assertEquals("CRITICAL", metrics.getRiskClassification());
        assertEquals("High Risk (<75%)", metrics.getStatus());
        assertEquals(0, metrics.getMaxCanMiss());
        // Required: ceil(((0.75 * 40) - 26) / (1 - 0.75)) = ceil((30 - 26) / 0.25) = ceil(4 / 0.25) = 16
        assertEquals(16, metrics.getMinRequiredToAttend());
    }

    @Test
    @DisplayName("calculateProjectionMetrics returns correct current, ifMissNext, ifAttendNext, fullProjection, disclaimer")
    void testProjectionMetrics() {
        ProjectionDataDto projection = attendanceService.calculateProjectionMetrics(40, 30, 2, 4, 75);

        assertNotNull(projection);
        assertNotNull(projection.getCurrent());
        assertEquals(75.00, projection.getCurrent().getPercentage());

        // ifMissNext scenario
        assertNotNull(projection.getIfMissNext());
        assertEquals(2, projection.getIfMissNext().getAdditionalMissed());
        // 30 / 42 = 71.43%
        assertEquals(71.43, projection.getIfMissNext().getProjectedPercentage());

        // ifAttendNext scenario
        assertNotNull(projection.getIfAttendNext());
        assertEquals(4, projection.getIfAttendNext().getAdditionalAttended());
        // 34 / 44 = 77.27%
        assertEquals(77.27, projection.getIfAttendNext().getProjectedPercentage());

        assertNotNull(projection.getFullProjection());
        assertEquals("Note: These calculations are mathematical projections for academic planning.", projection.getDisclaimer());
    }

    @Test
    @DisplayName("getAttendanceRecords returns scoped records for authenticated student")
    void testGetAttendanceRecordsSuccess() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(attendanceRepository.findByStudentId("std-1")).thenReturn(List.of(attendance1));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(mathSubject));

        List<AttendanceRecordDto> result = attendanceService.getAttendanceRecords("user-1", 75);

        assertEquals(1, result.size());
        AttendanceRecordDto item = result.get(0);
        assertEquals("att-1", item.getId());
        assertEquals("sub-1", item.getSubjectId());
        assertEquals("CS101", item.getSubjectCode());
        assertEquals("Calculus", item.getSubjectName());
        assertEquals("Dr. Ramanujan", item.getInstructor());
        assertEquals(90.00, item.getPercentage());
        assertEquals("SAFE", item.getRiskClassification());
    }

    @Test
    @DisplayName("updateAttendance succeeds when authenticated student owns the record")
    void testUpdateAttendanceSuccess() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(attendanceRepository.findById("att-1")).thenReturn(Optional.of(attendance1));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(mathSubject));

        AttendanceUpdateRequest request = new AttendanceUpdateRequest(42, 38);
        AttendanceRecordDto result = attendanceService.updateAttendance("user-1", "att-1", request);

        assertEquals(42, attendance1.getClassesConducted());
        assertEquals(38, attendance1.getClassesAttended());
        assertEquals(42, result.getConducted());
        assertEquals(38, result.getAttended());
        verify(attendanceRepository).save(attendance1);
    }

    @Test
    @DisplayName("updateAttendance rejects cross-student update with 403 Forbidden")
    void testUpdateAttendanceCrossStudentForbidden() {
        // Record belongs to std-1, but std-2 (user-2) is attempting to update it
        when(studentRepository.findByUserId("user-2")).thenReturn(Optional.of(student2));
        when(attendanceRepository.findById("att-1")).thenReturn(Optional.of(attendance1));

        AttendanceUpdateRequest request = new AttendanceUpdateRequest(50, 45);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.updateAttendance("user-2", "att-1", request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You do not own this attendance record"));
        verify(attendanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateAttendance returns 404 when attendance record does not exist")
    void testUpdateAttendanceNotFound() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(attendanceRepository.findById("non-existent")).thenReturn(Optional.empty());

        AttendanceUpdateRequest request = new AttendanceUpdateRequest(40, 30);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.updateAttendance("user-1", "non-existent", request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Attendance record not found"));
    }

    @Test
    @DisplayName("explainAttendance returns fallback explanation when Gemini returns fallback")
    void testExplainAttendanceFallback() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(mathSubject));
        when(attendanceRepository.findBySubjectIdAndStudentId("sub-1", "std-1")).thenReturn(Optional.of(attendance1));
        when(geminiService.generateExplanation(eq("CS101"), eq("Calculus"), any(AttendanceMetrics.class)))
                .thenReturn(new GeminiService.ExplanationResult("Your attendance in Calculus is at 90.0%...", true));

        AttendanceExplainRequest req = new AttendanceExplainRequest("sub-1", 40, 36, 75);
        ExplainDataDto result = attendanceService.explainAttendance("user-1", req);

        assertNotNull(result);
        assertEquals(90.00, result.getMetrics().getPercentage());
        assertTrue(result.isFallback());
        assertTrue(result.getExplanation().contains("Calculus"));
    }

    @Test
    @DisplayName("explainAttendance throws 404 when subject or record is not found for student")
    void testExplainAttendanceNotFound() {
        when(studentRepository.findByUserId("user-1")).thenReturn(Optional.of(student1));
        when(subjectRepository.findById("sub-invalid")).thenReturn(Optional.empty());

        AttendanceExplainRequest req = new AttendanceExplainRequest("sub-invalid", 40, 36, 75);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> attendanceService.explainAttendance("user-1", req));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Subject or attendance record not found for student"));
    }
}
