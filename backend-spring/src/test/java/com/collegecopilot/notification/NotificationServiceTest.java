package com.collegecopilot.notification;

import com.collegecopilot.auth.UserRepository;
import com.collegecopilot.notification.dto.*;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.student.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
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

    private NotificationService notificationService;

    private UserPrincipal studentA;
    private UserPrincipal studentB;
    private UserPrincipal adminUser;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository, studentRepository, userRepository,
                subjectRepository, attendanceRepository, assignmentRepository,
                examRepository, studyPlanRepository, studySessionRepository
        );

        studentA = new UserPrincipal("u-student-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);
        studentB = new UserPrincipal("u-student-2", "Bob Student", "bob@student.edu", "STUDENT", "std-2", null);
        adminUser = new UserPrincipal("u-admin-1", "Admin User", "admin@college.edu", "ADMIN", null, "adm-1");
    }

    @Test
    @DisplayName("getNotifications returns notifications and unread count for authenticated student")
    void getNotifications_returnsStudentNotifications() {
        Notification n1 = new Notification("n-1", "u-student-1", "Assignment Due", "Due in 2 days", "assignment", false);
        Notification n2 = new Notification("n-2", "u-student-1", "Exam Alert", "Exam next week", "exam", true);

        when(notificationRepository.findByUserIdOrStudentIdOrderByCreatedAtDesc("u-student-1", "std-1"))
                .thenReturn(List.of(n1, n2));

        NotificationListResponse response = notificationService.getNotifications(null, null, studentA);

        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().getCount());
        assertEquals(1, response.getData().getUnreadCount());
        assertEquals(2, response.getData().getNotifications().size());
        assertEquals("Assignment Due", response.getData().getNotifications().get(0).getTitle());
    }

    @Test
    @DisplayName("getNotifications filters by unread status and type")
    void getNotifications_filtersUnreadAndType() {
        Notification n1 = new Notification("n-1", "u-student-1", "Assignment Due", "Due in 2 days", "assignment", false);
        Notification n2 = new Notification("n-2", "u-student-1", "Exam Alert", "Exam next week", "exam", false);
        Notification n3 = new Notification("n-3", "u-student-1", "Assignment Done", "Submitted", "assignment", true);

        when(notificationRepository.findByUserIdOrStudentIdOrderByCreatedAtDesc("u-student-1", "std-1"))
                .thenReturn(List.of(n1, n2, n3));

        NotificationListResponse response = notificationService.getNotifications("unread", "assignment", studentA);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().getCount());
        assertEquals("n-1", response.getData().getNotifications().get(0).getId());
    }

    @Test
    @DisplayName("getUnreadCount returns correct unread count")
    void getUnreadCount_returnsCount() {
        Notification n1 = new Notification("n-1", "u-student-1", "Notice 1", "Msg 1", "exam", false);
        Notification n2 = new Notification("n-2", "u-student-1", "Notice 2", "Msg 2", "exam", false);
        Notification n3 = new Notification("n-3", "u-student-1", "Notice 3", "Msg 3", "exam", true);

        when(notificationRepository.findByUserIdOrStudentIdOrderByCreatedAtDesc("u-student-1", "std-1"))
                .thenReturn(List.of(n1, n2, n3));

        UnreadCountResponse response = notificationService.getUnreadCount(studentA);

        assertTrue(response.isSuccess());
        assertEquals(2, response.getCount());
    }

    @Test
    @DisplayName("markAsRead marks notification as read when owned by student")
    void markAsRead_byOwner_success() {
        Notification n = new Notification("n-1", "u-student-1", "Exam Alert", "Msg", "exam", false);
        when(notificationRepository.findById("n-1")).thenReturn(Optional.of(n));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        NotificationResponse res = notificationService.markAsRead("n-1", studentA);

        assertTrue(res.isSuccess());
        assertTrue(res.getData().getIsRead());
        verify(notificationRepository, times(1)).save(n);
    }

    @Test
    @DisplayName("markAsRead on another student's notification throws 403 Forbidden")
    void markAsRead_crossStudent_throws403() {
        Notification n = new Notification("n-1", "u-student-1", "Exam Alert", "Msg", "exam", false);
        when(notificationRepository.findById("n-1")).thenReturn(Optional.of(n));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                notificationService.markAsRead("n-1", studentB));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Access denied"));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("markAsRead on non-existent notification throws 404 Not Found")
    void markAsRead_notFound_throws404() {
        when(notificationRepository.findById("missing-id")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                notificationService.markAsRead("missing-id", studentA));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("markAllAsRead marks all unread notifications for student as read")
    void markAllAsRead_success() {
        Notification n1 = new Notification("n-1", "u-student-1", "Notice 1", "Msg 1", "exam", false);
        Notification n2 = new Notification("n-2", "u-student-1", "Notice 2", "Msg 2", "exam", false);

        when(notificationRepository.findByUserIdOrStudentIdOrderByCreatedAtDesc("u-student-1", "std-1"))
                .thenReturn(List.of(n1, n2));

        NotificationReadAllResponse res = notificationService.markAllAsRead(studentA);

        assertTrue(res.isSuccess());
        assertEquals(2, res.getUpdatedCount());
        verify(notificationRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("deleteNotification deletes notification owned by student")
    void deleteNotification_byOwner_success() {
        Notification n = new Notification("n-1", "u-student-1", "Notice", "Msg", "exam", false);
        when(notificationRepository.findById("n-1")).thenReturn(Optional.of(n));

        NotificationDeleteResponse res = notificationService.deleteNotification("n-1", studentA);

        assertTrue(res.isSuccess());
        assertEquals("Notification deleted successfully", res.getMessage());
        verify(notificationRepository, times(1)).delete(n);
    }

    @Test
    @DisplayName("deleteNotification on another student's notification throws 403 Forbidden")
    void deleteNotification_crossStudent_throws403() {
        Notification n = new Notification("n-1", "u-student-1", "Notice", "Msg", "exam", false);
        when(notificationRepository.findById("n-1")).thenReturn(Optional.of(n));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                notificationService.deleteNotification("n-1", studentB));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteNotification on non-existent notification throws 404 Not Found")
    void deleteNotification_notFound_throws404() {
        when(notificationRepository.findById("missing-id")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                notificationService.deleteNotification("missing-id", studentA));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("generateNotifications student cannot generate for another student (throws 403)")
    void generateNotifications_crossStudent_throws403() {
        NotificationGenerateRequest req = new NotificationGenerateRequest("std-2");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                notificationService.generateNotifications(req, studentA));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Students cannot generate notifications for other students"));
    }

    @Test
    @DisplayName("generateNotifications by admin for all students succeeds")
    void generateNotifications_byAdminForAll_success() {
        Student s1 = new Student("std-1", "u-student-1", "CS-1", 4, "CS", null);
        when(studentRepository.findAll()).thenReturn(List.of(s1));
        when(attendanceRepository.findByStudentId("std-1")).thenReturn(List.of());
        when(assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc("std-1")).thenReturn(List.of());
        when(examRepository.findByStudentIdOrLegacyOrderByExamDateAsc("std-1")).thenReturn(List.of());
        when(studyPlanRepository.findByStudentId("std-1")).thenReturn(List.of());

        NotificationGenerateResponse res = notificationService.generateNotifications(null, adminUser);

        assertTrue(res.isSuccess());
        assertTrue(res.getMessage().contains("Generated notifications for all students"));
    }
}
