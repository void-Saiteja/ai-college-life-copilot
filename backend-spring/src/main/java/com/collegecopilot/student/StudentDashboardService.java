package com.collegecopilot.student;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.auth.User;
import com.collegecopilot.auth.UserRepository;
import com.collegecopilot.student.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudentDashboardService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final ExamRepository examRepository;
    private final StudyPlanRepository studyPlanRepository;
    private final StudySessionRepository studySessionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final NotificationRepository notificationRepository;

    public StudentDashboardService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            AttendanceRepository attendanceRepository,
            AssignmentRepository assignmentRepository,
            ExamRepository examRepository,
            StudyPlanRepository studyPlanRepository,
            StudySessionRepository studySessionRepository,
            QuizAttemptRepository quizAttemptRepository,
            NotificationRepository notificationRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceRepository = attendanceRepository;
        this.assignmentRepository = assignmentRepository;
        this.examRepository = examRepository;
        this.studyPlanRepository = studyPlanRepository;
        this.studySessionRepository = studySessionRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public StudentDashboardResponse getStudentDashboard(String authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }

        // 1. Resolve student strictly through authenticated user ID (never trusting client parameters)
        Student student = studentRepository.findByUserId(authenticatedUserId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "Student profile not found for authenticated user"));

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "User profile not found"));

        DashboardDataDto data = new DashboardDataDto();

        // 2. Student Profile Summary
        StudentProfileDto studentProfile = new StudentProfileDto(
                student.getId(),
                user.getName(),
                user.getEmail(),
                student.getSemester(),
                student.getDepartment(),
                student.getGpa()
        );
        data.setStudent(studentProfile);

        // 3. Subjects
        List<Subject> allSubjects = subjectRepository.findAll();
        Map<String, Subject> subjectMap = allSubjects.stream()
                .collect(Collectors.toMap(Subject::getId, Function.identity(), (a, b) -> a));

        List<SubjectDto> subjectDtos = allSubjects.stream()
                .map(s -> new SubjectDto(s.getId(), s.getCode(), s.getName(), s.getInstructor(), s.getCredits(), s.getDepartment()))
                .collect(Collectors.toList());
        data.setSubjects(subjectDtos);

        // 4. Attendance
        List<Attendance> attendanceRecords = attendanceRepository.findByStudentId(student.getId());
        List<AttendanceDto> attendanceDtos = attendanceRecords.stream().map(a -> {
            Subject sub = subjectMap.get(a.getSubjectId());
            int conducted = a.getClassesConducted();
            int attended = a.getClassesAttended();
            int pct = conducted > 0 ? (int) Math.round((attended * 100.0) / conducted) : 0;
            String status = pct >= 75 ? "Good Standing" : "Low Attendance Warning";

            return new AttendanceDto(
                    sub != null ? sub.getName() : "Subject",
                    sub != null ? sub.getCode() : "CS",
                    conducted,
                    attended,
                    pct,
                    status
            );
        }).collect(Collectors.toList());
        data.setAttendance(attendanceDtos);

        // 5. Upcoming Assignments
        LocalDate today = LocalDate.now();
        List<Assignment> assignments = assignmentRepository.findByStudentIdAndStatusNotOrderByDueDateAsc(student.getId(), "Completed");
        List<AssignmentSummaryDto> assignmentDtos = assignments.stream().map(a -> {
            Subject sub = subjectMap.get(a.getSubjectId());
            int daysRemaining = (int) ChronoUnit.DAYS.between(today, a.getDueDate());

            String deadlineStatus;
            if (daysRemaining < 0) {
                deadlineStatus = "OVERDUE";
            } else if (daysRemaining == 0) {
                deadlineStatus = "DUE_TODAY";
            } else if (daysRemaining <= 3) {
                deadlineStatus = "DUE_SOON";
            } else {
                deadlineStatus = "UPCOMING";
            }

            String urgency;
            if ("Completed".equalsIgnoreCase(a.getStatus())) {
                urgency = "COMPLETED";
            } else if (daysRemaining <= 0) {
                urgency = "CRITICAL";
            } else if (daysRemaining <= 3) {
                urgency = "HIGH";
            } else if (daysRemaining <= 7) {
                urgency = "MEDIUM";
            } else {
                urgency = "LOW";
            }

            AssignmentSummaryDto dto = new AssignmentSummaryDto();
            dto.setId(a.getId());
            dto.setTitle(a.getTitle());
            dto.setSubject(sub != null ? sub.getName() : "Course");
            dto.setDueDate(a.getDueDate().toString());
            dto.setPriority(urgency);
            dto.setUrgency(urgency);
            dto.setDaysRemaining(daysRemaining);
            dto.setDeadlineStatus(deadlineStatus);
            dto.setEstimatedHours(a.getEstimatedHours());
            return dto;
        }).collect(Collectors.toList());
        data.setUpcomingAssignments(assignmentDtos);

        // 6. Upcoming Exams
        List<Exam> exams = examRepository.findByStudentIdOrderByExamDateAsc(student.getId());
        List<ExamSummaryDto> examDtos = exams.stream().map(e -> {
            Subject sub = subjectMap.get(e.getSubjectId());
            int daysRemaining = (int) ChronoUnit.DAYS.between(today, e.getExamDate());

            String examStatus;
            String urgency;
            if (daysRemaining < 0) {
                examStatus = "COMPLETED";
                urgency = "COMPLETED";
            } else if (daysRemaining == 0) {
                examStatus = "TODAY";
                urgency = "CRITICAL";
            } else if (daysRemaining <= 3) {
                examStatus = "VERY_SOON";
                urgency = "HIGH";
            } else if (daysRemaining <= 7) {
                examStatus = "UPCOMING";
                urgency = "MEDIUM";
            } else {
                examStatus = "UPCOMING";
                urgency = "LOW";
            }

            ExamSummaryDto dto = new ExamSummaryDto();
            dto.setId(e.getId());
            dto.setTitle(e.getTitle());
            dto.setSubject(sub != null ? sub.getName() : "Course");
            dto.setSubjectCode(sub != null ? sub.getCode() : "CS");
            dto.setExamDate(e.getExamDate().toString());
            dto.setStartTime(e.getStartTime() != null ? e.getStartTime().toString() : "");
            dto.setLocation(e.getLocation());
            dto.setSyllabus(e.getSyllabus());
            dto.setDaysRemaining(daysRemaining);
            dto.setExamStatus(examStatus);
            dto.setUrgency(urgency);
            return dto;
        }).collect(Collectors.toList());
        data.setUpcomingExams(examDtos);

        // 7. Active Study Plan
        Optional<StudyPlan> activePlanOpt = studyPlanRepository.findFirstByStudentIdAndStatus(student.getId(), "Active");
        List<Object> sessions = activePlanOpt
                .map(plan -> studySessionRepository.findByPlanIdOrderBySessionDateAscStartTimeAsc(plan.getId()).stream().map(s -> (Object) s).toList())
                .orElse(Collections.emptyList());
        data.setStudyPlan(new StudyPlanDto(activePlanOpt.orElse(null), sessions));

        // 8. Quiz Attempts & History Summary
        List<QuizAttempt> quizAttempts = quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        if (!quizAttempts.isEmpty()) {
            QuizAttempt recent = quizAttempts.get(0);
            data.setRecentQuizScore(String.format("%.0f%% in %s", recent.getPercentage(), recent.getTopic()));
        } else {
            data.setRecentQuizScore("No quiz attempts recorded");
        }

        // Compute weak topics (<60%)
        Map<String, List<BigDecimal>> topicScores = new HashMap<>();
        for (QuizAttempt qa : quizAttempts) {
            String topicKey = qa.getTopic() != null ? qa.getTopic() : "General";
            topicScores.computeIfAbsent(topicKey, k -> new ArrayList<>()).add(qa.getPercentage());
        }
        List<String> weakTopics = new ArrayList<>();
        topicScores.forEach((topic, scores) -> {
            double avg = scores.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
            if (avg < 60.0) {
                weakTopics.add(topic);
            }
        });
        data.setQuizHistorySummary(new QuizHistorySummaryDto(
                quizAttempts.size(),
                weakTopics,
                !quizAttempts.isEmpty() ? quizAttempts.get(0) : null
        ));

        // 9. Notifications
        List<Notification> notifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(authenticatedUserId);
        List<NotificationDto> notificationDtos = notifs.stream().map(n -> new NotificationDto(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.getIsRead()
        )).collect(Collectors.toList());
        data.setNotifications(notificationDtos);

        // Career Profile Summary defaults to null if not populated
        data.setCareerProfileSummary(null);

        return new StudentDashboardResponse(true, data);
    }
}
