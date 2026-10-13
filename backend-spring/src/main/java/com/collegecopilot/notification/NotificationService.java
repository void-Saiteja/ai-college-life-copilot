package com.collegecopilot.notification;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.auth.User;
import com.collegecopilot.auth.UserRepository;
import com.collegecopilot.notification.dto.*;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.student.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final ExamRepository examRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final StudySessionRepository studySessionRepository;

    @Autowired
    public NotificationService(
            NotificationRepository notificationRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            AttendanceRepository attendanceRepository,
            AssignmentRepository assignmentRepository,
            ExamRepository examRepository,
            StudyPlanRepository studyPlanRepository,
            StudySessionRepository studySessionRepository) {
        this.notificationRepository = notificationRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceRepository = attendanceRepository;
        this.assignmentRepository = assignmentRepository;
        this.examRepository = examRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.studySessionRepository = studySessionRepository;
    }

    private void validateAuthenticatedStudent(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        String uid = principal.getId();
        String sid = principal.getStudentId();
        if ((uid == null || uid.isBlank()) && (sid == null || sid.isBlank())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
    }

    private boolean isOwner(Notification n, UserPrincipal principal) {
        if (n == null || principal == null) return false;
        String notifUserId = n.getUserId();
        if (notifUserId == null) return false;

        if (notifUserId.equals(principal.getId())) return true;
        if (principal.getStudentId() != null && notifUserId.equals(principal.getStudentId())) return true;

        // Legacy mapping if std-1
        if ("std-1".equals(principal.getStudentId()) && ("u-student-1".equals(notifUserId) || "std-1".equals(notifUserId))) {
            return true;
        }

        return false;
    }

    public NotificationListResponse getNotifications(String status, String type, UserPrincipal principal) {
        validateAuthenticatedStudent(principal);

        String userId = principal.getId();
        String studentId = principal.getStudentId();

        List<Notification> all = notificationRepository.findByUserIdOrStudentIdOrderByCreatedAtDesc(userId, studentId);

        // Filter by status
        List<Notification> filtered = all;
        if ("unread".equalsIgnoreCase(status)) {
            filtered = filtered.stream()
                    .filter(n -> !Boolean.TRUE.equals(n.getIsRead()))
                    .collect(Collectors.toList());
        } else if ("read".equalsIgnoreCase(status)) {
            filtered = filtered.stream()
                    .filter(n -> Boolean.TRUE.equals(n.getIsRead()))
                    .collect(Collectors.toList());
        }

        // Filter by type
        if (type != null && !type.isBlank()) {
            filtered = filtered.stream()
                    .filter(n -> n.getType() != null && n.getType().equalsIgnoreCase(type.trim()))
                    .collect(Collectors.toList());
        }

        List<NotificationItemDto> items = filtered.stream()
                .map(n -> toDto(n, principal))
                .collect(Collectors.toList());

        int unreadCount = (int) all.stream()
                .filter(n -> !Boolean.TRUE.equals(n.getIsRead()))
                .count();

        return new NotificationListResponse(true, new NotificationListData(items, unreadCount, items.size()));
    }

    public UnreadCountResponse getUnreadCount(UserPrincipal principal) {
        validateAuthenticatedStudent(principal);

        String userId = principal.getId();
        String studentId = principal.getStudentId();

        List<Notification> all = notificationRepository.findByUserIdOrStudentIdOrderByCreatedAtDesc(userId, studentId);
        int unreadCount = (int) all.stream()
                .filter(n -> !Boolean.TRUE.equals(n.getIsRead()))
                .count();

        return new UnreadCountResponse(true, unreadCount);
    }

    @Transactional
    public NotificationResponse markAsRead(String id, UserPrincipal principal) {
        validateAuthenticatedStudent(principal);

        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!isOwner(n, principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: Access denied to notification");
        }

        n.setIsRead(true);
        Notification saved = notificationRepository.save(n);
        return new NotificationResponse(true, "Notification marked as read", toDto(saved, principal));
    }

    @Transactional
    public NotificationReadAllResponse markAllAsRead(UserPrincipal principal) {
        validateAuthenticatedStudent(principal);

        String userId = principal.getId();
        String studentId = principal.getStudentId();

        List<Notification> all = notificationRepository.findByUserIdOrStudentIdOrderByCreatedAtDesc(userId, studentId);
        List<Notification> unreadList = all.stream()
                .filter(n -> !Boolean.TRUE.equals(n.getIsRead()))
                .toList();

        unreadList.forEach(n -> n.setIsRead(true));
        if (!unreadList.isEmpty()) {
            notificationRepository.saveAll(unreadList);
        }

        return new NotificationReadAllResponse(true, "All notifications marked as read", unreadList.size());
    }

    @Transactional
    public NotificationDeleteResponse deleteNotification(String id, UserPrincipal principal) {
        validateAuthenticatedStudent(principal);

        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!isOwner(n, principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: Access denied to notification");
        }

        notificationRepository.delete(n);
        return new NotificationDeleteResponse(true, "Notification deleted successfully");
    }

    @Transactional
    public NotificationGenerateResponse generateNotifications(NotificationGenerateRequest request, UserPrincipal principal) {
        String targetStudentId = request != null ? request.getStudentId() : null;
        String currentStudentId = principal.getStudentId();
        boolean isAdmin = "ADMIN".equalsIgnoreCase(principal.getRole());

        // Student A cannot generate notifications for Student B
        if (!isAdmin && targetStudentId != null && !targetStudentId.isBlank() && !targetStudentId.equals(currentStudentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: Students cannot generate notifications for other students");
        }

        if (!isAdmin && (currentStudentId == null || currentStudentId.isBlank())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }

        // Admin generating for all students
        if (isAdmin && (targetStudentId == null || targetStudentId.isBlank())) {
            List<Student> students = studentRepository.findAll();
            int totalCreated = 0;
            for (Student s : students) {
                totalCreated += generateForStudent(s.getId(), s.getUserId());
            }
            return new NotificationGenerateResponse(true, "Generated notifications for all students", totalCreated);
        }

        String studentToGenerate = isAdmin ? targetStudentId : currentStudentId;
        Student student = resolveStudent(studentToGenerate);
        String userIdToGenerate = student != null ? student.getUserId() : principal.getId();

        int createdCount = generateForStudent(studentToGenerate, userIdToGenerate);

        List<Notification> all = notificationRepository.findByUserIdOrStudentIdOrderByCreatedAtDesc(userIdToGenerate, studentToGenerate);
        int unreadCount = (int) all.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();

        return new NotificationGenerateResponse(true, new NotificationGenerateData(createdCount, all.size(), unreadCount));
    }

    private int generateForStudent(String studentId, String userId) {
        List<Notification> existing = notificationRepository.findByUserIdOrStudentIdOrderByCreatedAtDesc(userId, studentId);
        Set<String> existingTitles = existing.stream()
                .map(n -> n.getTitle() != null ? n.getTitle().trim().toLowerCase() : "")
                .collect(Collectors.toSet());

        List<Notification> newNotifs = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 1. Attendance Notifications
        List<Attendance> attendanceList = attendanceRepository.findByStudentId(studentId);
        Map<String, Subject> subjectMap = subjectRepository.findAll().stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (s1, s2) -> s1));

        for (Attendance att : attendanceList) {
            Subject sub = subjectMap.get(att.getSubjectId());
            String subCode = sub != null ? sub.getCode() : "CS";
            String subName = sub != null ? sub.getName() : "Subject";

            int conducted = att.getClassesConducted();
            int attended = att.getClassesAttended();
            if (conducted > 0) {
                int pct = (int) Math.round((attended * 100.0) / conducted);
                if (pct < 75) {
                    String title = "Attendance Critical Alert: " + subCode;
                    if (!existingTitles.contains(title.toLowerCase())) {
                        newNotifs.add(new Notification(
                                generateId(), userId, title,
                                "Your attendance in " + subName + " (" + subCode + ") is " + pct + "%, below the required 75% target.",
                                "attendance", false
                        ));
                        existingTitles.add(title.toLowerCase());
                    }
                } else if (pct < 80) {
                    String title = "Attendance Warning: " + subCode;
                    if (!existingTitles.contains(title.toLowerCase())) {
                        newNotifs.add(new Notification(
                                generateId(), userId, title,
                                "Your attendance in " + subName + " (" + subCode + ") is " + pct + "%. You are close to the 75% target.",
                                "attendance", false
                        ));
                        existingTitles.add(title.toLowerCase());
                    }
                }
            }
        }

        // 2. Assignment Notifications
        List<Assignment> assignments = assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc(studentId);
        for (Assignment asg : assignments) {
            if ("completed".equalsIgnoreCase(asg.getStatus())) continue;
            LocalDate dueDate = asg.getDueDate();
            if (dueDate != null) {
                long diff = ChronoUnit.DAYS.between(today, dueDate);
                if (diff < 0) {
                    String title = "Overdue Assignment: " + asg.getTitle();
                    if (!existingTitles.contains(title.toLowerCase())) {
                        newNotifs.add(new Notification(
                                generateId(), userId, title,
                                "Assignment \"" + asg.getTitle() + "\" is overdue. Please submit as soon as possible.",
                                "assignment", false
                        ));
                        existingTitles.add(title.toLowerCase());
                    }
                } else if (diff == 0) {
                    String title = "Assignment Due Today: " + asg.getTitle();
                    if (!existingTitles.contains(title.toLowerCase())) {
                        newNotifs.add(new Notification(
                                generateId(), userId, title,
                                "Assignment \"" + asg.getTitle() + "\" is due today. Ensure your submission is completed.",
                                "assignment", false
                        ));
                        existingTitles.add(title.toLowerCase());
                    }
                } else if (diff <= 3) {
                    String title = "Assignment Due Soon: " + asg.getTitle();
                    if (!existingTitles.contains(title.toLowerCase())) {
                        newNotifs.add(new Notification(
                                generateId(), userId, title,
                                "Assignment \"" + asg.getTitle() + "\" is due in " + diff + " days (" + dueDate + ").",
                                "assignment", false
                        ));
                        existingTitles.add(title.toLowerCase());
                    }
                }
            }
        }

        // 3. Exam Notifications
        List<Exam> exams = examRepository.findByStudentIdOrLegacyOrderByExamDateAsc(studentId);
        for (Exam exam : exams) {
            if ("completed".equalsIgnoreCase(exam.getStatus())) continue;
            LocalDate examDate = exam.getExamDate();
            if (examDate != null) {
                long diff = ChronoUnit.DAYS.between(today, examDate);
                if (diff == 0) {
                    String title = "Exam Today: " + exam.getTitle();
                    if (!existingTitles.contains(title.toLowerCase())) {
                        newNotifs.add(new Notification(
                                generateId(), userId, title,
                                "Your " + exam.getTitle() + " examination is scheduled for today at " + (exam.getStartTime() != null ? exam.getStartTime() : "09:00 AM") + ".",
                                "exam", false
                        ));
                        existingTitles.add(title.toLowerCase());
                    }
                } else if (diff <= 3 && diff > 0) {
                    String title = "Upcoming Exam in " + diff + " Days: " + exam.getTitle();
                    if (!existingTitles.contains(title.toLowerCase())) {
                        newNotifs.add(new Notification(
                                generateId(), userId, title,
                                "Your " + exam.getTitle() + " exam is scheduled in " + diff + " days (" + examDate + ").",
                                "exam", false
                        ));
                        existingTitles.add(title.toLowerCase());
                    }
                } else if (diff <= 7 && diff > 3) {
                    String title = "Upcoming Exam: " + exam.getTitle();
                    if (!existingTitles.contains(title.toLowerCase())) {
                        newNotifs.add(new Notification(
                                generateId(), userId, title,
                                "Your " + exam.getTitle() + " exam is scheduled in " + diff + " days.",
                                "exam", false
                        ));
                        existingTitles.add(title.toLowerCase());
                    }
                }
            }
        }

        // 4. Study Plan Notifications
        studyPlanRepository.findByStudentId(studentId).stream()
                .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
                .findFirst()
                .ifPresent(plan -> {
                    List<StudySession> sessions = studySessionRepository.findByPlanIdOrderBySessionDateAscStartTimeAsc(plan.getId());
                    for (StudySession sess : sessions) {
                        if ("completed".equalsIgnoreCase(sess.getStatus())) continue;
                        if (today.equals(sess.getSessionDate())) {
                            String title = "Scheduled Study Session Today: " + sess.getTopic();
                            if (!existingTitles.contains(title.toLowerCase())) {
                                newNotifs.add(new Notification(
                                        generateId(), userId, title,
                                        "Your scheduled study session for \"" + sess.getTopic() + "\" starts today.",
                                        "planner", false
                                ));
                                existingTitles.add(title.toLowerCase());
                            }
                        }
                    }
                });

        if (!newNotifs.isEmpty()) {
            notificationRepository.saveAll(newNotifs);
        }

        return newNotifs.size();
    }

    private Student resolveStudent(String idOrUserId) {
        if (idOrUserId == null || idOrUserId.isBlank()) return null;
        Optional<Student> direct = studentRepository.findById(idOrUserId);
        if (direct.isPresent()) return direct.get();
        return studentRepository.findByUserId(idOrUserId).orElse(null);
    }

    private String generateId() {
        return "notif-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    private String derivePriority(String title, String message) {
        String combined = ((title != null ? title : "") + " " + (message != null ? message : "")).toUpperCase();
        if (combined.contains("CRITICAL") || combined.contains("DUE TODAY") || combined.contains("EXAM TODAY") || combined.contains("OVERDUE")) {
            return "CRITICAL";
        }
        if (combined.contains("WARNING") || combined.contains("DUE SOON") || combined.contains("UPCOMING") || combined.contains("HIGH")) {
            return "HIGH";
        }
        if (combined.contains("LOW")) {
            return "LOW";
        }
        return "MEDIUM";
    }

    private NotificationItemDto toDto(Notification n, UserPrincipal principal) {
        String priority = derivePriority(n.getTitle(), n.getMessage());
        String typeUpper = n.getType() != null ? n.getType().toUpperCase() : "GENERAL";
        String studentId = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        String createdAtStr = n.getCreatedAt() != null
                ? n.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return new NotificationItemDto(
                n.getId(),
                studentId,
                n.getUserId(),
                n.getTitle(),
                n.getMessage(),
                typeUpper,
                priority,
                Boolean.TRUE.equals(n.getIsRead()),
                createdAtStr
        );
    }
}
