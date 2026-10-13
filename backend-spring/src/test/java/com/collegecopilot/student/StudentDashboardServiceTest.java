package com.collegecopilot.student;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.auth.Role;
import com.collegecopilot.auth.User;
import com.collegecopilot.auth.UserRepository;
import com.collegecopilot.student.dto.StudentDashboardResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentDashboardServiceTest {

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
    private StudyPlanRepository studyPlanRepository;

    @Mock
    private StudySessionRepository studySessionRepository;

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private StudentDashboardService dashboardService;

    @Test
    @DisplayName("Authenticated student retrieves their own complete dashboard metrics")
    void testGetStudentDashboardSuccess() {
        String userId = "u-student-1";
        String studentId = "std-1";

        User user = new User(userId, "Alex Mercer", "alex@student.edu", "pass_hash", Role.STUDENT);
        Student student = new Student(studentId, userId, "CS-2026-88", 4, "Computer Science & Engineering", new BigDecimal("3.85"));

        Subject sub1 = new Subject("sub-1", "CS 201", "Data Structures & Algorithms", "Dr. Alan Turing", 4, "Computer Science");
        Subject sub2 = new Subject("sub-2", "CS 340", "Database Systems", "Dr. Grace Hopper", 3, "Computer Science");

        Attendance att1 = new Attendance("att-1", studentId, "sub-1", 28, 25);
        Attendance att2 = new Attendance("att-2", studentId, "sub-2", 20, 14);

        Assignment asg1 = new Assignment("asg-1", studentId, "sub-1", "B-Tree Project", LocalDate.now().plusDays(2), "High", "In Progress");
        Exam exam1 = new Exam("ex-1", studentId, "sub-1", "DSA Midterm", LocalDate.now().plusDays(5), LocalTime.of(9, 0), "Hall A");

        QuizAttempt qa1 = new QuizAttempt("qa-1", studentId, "sub-1", "Trees", 4, 5, new BigDecimal("80.00"));
        QuizAttempt qa2 = new QuizAttempt("qa-2", studentId, "sub-1", "Graphs", 2, 5, new BigDecimal("40.00"));

        Notification notif = new Notification("n-1", userId, "Exam Notice", "DSA exam in 5 days", "exam", false);

        when(studentRepository.findByUserId(userId)).thenReturn(Optional.of(student));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(subjectRepository.findAll()).thenReturn(List.of(sub1, sub2));
        when(attendanceRepository.findByStudentId(studentId)).thenReturn(List.of(att1, att2));
        when(assignmentRepository.findByStudentIdAndStatusNotOrderByDueDateAsc(studentId, "Completed")).thenReturn(List.of(asg1));
        when(examRepository.findByStudentIdOrderByExamDateAsc(studentId)).thenReturn(List.of(exam1));
        when(studyPlanRepository.findFirstByStudentIdAndStatus(studentId, "Active")).thenReturn(Optional.empty());
        when(quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc(studentId)).thenReturn(List.of(qa1, qa2));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(notif));

        StudentDashboardResponse response = dashboardService.getStudentDashboard(userId);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();

        // Verify Student Profile
        assertThat(response.getData().getStudent().getId()).isEqualTo(studentId);
        assertThat(response.getData().getStudent().getName()).isEqualTo("Alex Mercer");
        assertThat(response.getData().getStudent().getEmail()).isEqualTo("alex@student.edu");
        assertThat(response.getData().getStudent().getSemester()).isEqualTo(4);

        // Verify Attendance Calculation
        assertThat(response.getData().getAttendance()).hasSize(2);
        assertThat(response.getData().getAttendance().get(0).getPercentage()).isEqualTo(89);
        assertThat(response.getData().getAttendance().get(0).getStatus()).isEqualTo("Good Standing");
        assertThat(response.getData().getAttendance().get(1).getPercentage()).isEqualTo(70);
        assertThat(response.getData().getAttendance().get(1).getStatus()).isEqualTo("Low Attendance Warning");

        // Verify Upcoming Assignments & Urgency Calculation
        assertThat(response.getData().getUpcomingAssignments()).hasSize(1);
        assertThat(response.getData().getUpcomingAssignments().get(0).getDaysRemaining()).isEqualTo(2);
        assertThat(response.getData().getUpcomingAssignments().get(0).getDeadlineStatus()).isEqualTo("DUE_SOON");
        assertThat(response.getData().getUpcomingAssignments().get(0).getPriority()).isEqualTo("HIGH");

        // Verify Upcoming Exams
        assertThat(response.getData().getUpcomingExams()).hasSize(1);
        assertThat(response.getData().getUpcomingExams().get(0).getTitle()).isEqualTo("DSA Midterm");
        assertThat(response.getData().getUpcomingExams().get(0).getDaysRemaining()).isEqualTo(5);
        assertThat(response.getData().getUpcomingExams().get(0).getExamStatus()).isEqualTo("UPCOMING");

        // Verify Quiz Metrics & Weak Topics (< 60%)
        assertThat(response.getData().getRecentQuizScore()).contains("80%");
        assertThat(response.getData().getQuizHistorySummary().getWeakTopics()).contains("Graphs");

        // Verify Notifications
        assertThat(response.getData().getNotifications()).hasSize(1);
        assertThat(response.getData().getNotifications().get(0).getTitle()).isEqualTo("Exam Notice");

        // Confirm ownership: queried strictly with authenticated student's ID
        verify(studentRepository).findByUserId(userId);
        verify(attendanceRepository).findByStudentId(studentId);
        verify(assignmentRepository).findByStudentIdAndStatusNotOrderByDueDateAsc(studentId, "Completed");
        verify(examRepository).findByStudentIdOrderByExamDateAsc(studentId);
    }

    @Test
    @DisplayName("Throws 404 when student profile does not exist for authenticated user")
    void testGetStudentDashboardMissingStudentRecord() {
        when(studentRepository.findByUserId("u-unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getStudentDashboard("u-unknown"))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException authEx = (AuthException) ex;
                    assertThat(authEx.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(authEx.getMessage()).isEqualTo("Student profile not found for authenticated user");
                });

        verify(attendanceRepository, never()).findByStudentId(anyString());
    }

    @Test
    @DisplayName("Throws 401 when authenticated user ID is null or blank")
    void testGetStudentDashboardUnauthenticated() {
        assertThatThrownBy(() -> dashboardService.getStudentDashboard(null))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException authEx = (AuthException) ex;
                    assertThat(authEx.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });

        assertThatThrownBy(() -> dashboardService.getStudentDashboard("   "))
                .isInstanceOf(AuthException.class)
                .satisfies(ex -> {
                    AuthException authEx = (AuthException) ex;
                    assertThat(authEx.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("Student A cannot access Student B's data (strictly scoped to authenticated ID)")
    void testStudentOwnershipStrictIsolation() {
        String studentAUserId = "u-student-A";
        String studentAId = "std-A";
        String studentBUserId = "u-student-B";

        Student studentA = new Student(studentAId, studentAUserId, "CS-1111", 1, "CS", new BigDecimal("3.50"));
        User userA = new User(studentAUserId, "Student A", "a@student.edu", "pass", Role.STUDENT);

        when(studentRepository.findByUserId(studentAUserId)).thenReturn(Optional.of(studentA));
        when(userRepository.findById(studentAUserId)).thenReturn(Optional.of(userA));
        when(subjectRepository.findAll()).thenReturn(List.of());
        when(attendanceRepository.findByStudentId(studentAId)).thenReturn(List.of());
        when(assignmentRepository.findByStudentIdAndStatusNotOrderByDueDateAsc(studentAId, "Completed")).thenReturn(List.of());
        when(examRepository.findByStudentIdOrderByExamDateAsc(studentAId)).thenReturn(List.of());
        when(studyPlanRepository.findFirstByStudentIdAndStatus(studentAId, "Active")).thenReturn(Optional.empty());
        when(quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc(studentAId)).thenReturn(List.of());
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(studentAUserId)).thenReturn(List.of());

        StudentDashboardResponse responseA = dashboardService.getStudentDashboard(studentAUserId);

        assertThat(responseA.getData().getStudent().getId()).isEqualTo(studentAId);

        // Verify that student B's data was NEVER requested
        verify(studentRepository, never()).findByUserId(studentBUserId);
        verify(attendanceRepository, never()).findByStudentId("std-B");
    }
}
