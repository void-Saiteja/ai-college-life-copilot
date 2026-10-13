package com.collegecopilot.assignment;

import com.collegecopilot.assignment.dto.*;
import com.collegecopilot.student.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ExamRepository examRepository;
    private final AttendanceRepository attendanceRepository;
    private final GeminiAssignmentAdvisorService advisorService;

    public AssignmentService(
            AssignmentRepository assignmentRepository,
            StudentRepository studentRepository,
            SubjectRepository subjectRepository,
            ExamRepository examRepository,
            AttendanceRepository attendanceRepository,
            GeminiAssignmentAdvisorService advisorService) {
        this.assignmentRepository = assignmentRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.examRepository = examRepository;
        this.attendanceRepository = attendanceRepository;
        this.advisorService = advisorService;
    }

    public AssignmentMetrics calculateAssignmentMetrics(Assignment assignment) {
        return calculateAssignmentMetrics(assignment, LocalDate.now());
    }

    public AssignmentMetrics calculateAssignmentMetrics(Assignment assignment, LocalDate today) {
        String statusRaw = (assignment.getStatus() != null ? assignment.getStatus() : "Pending").trim();
        boolean isCompleted = "completed".equalsIgnoreCase(statusRaw);

        if (isCompleted) {
            return new AssignmentMetrics(0, "COMPLETED", "COMPLETED");
        }

        LocalDate dueDate = assignment.getDueDate();
        if (dueDate == null) {
            return new AssignmentMetrics(999, "UPCOMING", "LOW");
        }

        int daysRemaining = (int) ChronoUnit.DAYS.between(today, dueDate);

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
        if (daysRemaining <= 0) {
            urgency = "CRITICAL";
        } else if (daysRemaining <= 3) {
            urgency = "HIGH";
        } else if (daysRemaining <= 7) {
            urgency = "MEDIUM";
        } else {
            urgency = "LOW";
        }

        return new AssignmentMetrics(daysRemaining, deadlineStatus, urgency);
    }

    @Transactional(readOnly = true)
    public AssignmentListResponse getAssignments(String userId, String statusFilter) {
        Student student = getStudentByUserId(userId);
        List<Assignment> userAssignments = assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc(student.getId());

        List<AssignmentDto> formatted = userAssignments.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        String sLower = statusFilter != null ? statusFilter.toLowerCase().trim() : "all";
        List<AssignmentDto> filtered = formatted;

        switch (sLower) {
            case "pending" -> filtered = formatted.stream()
                    .filter(a -> !"COMPLETED".equalsIgnoreCase(a.getDeadlineStatus()) && !"Completed".equalsIgnoreCase(a.getStatus()))
                    .collect(Collectors.toList());
            case "completed" -> filtered = formatted.stream()
                    .filter(a -> "COMPLETED".equalsIgnoreCase(a.getDeadlineStatus()) || "Completed".equalsIgnoreCase(a.getStatus()))
                    .collect(Collectors.toList());
            case "overdue" -> filtered = formatted.stream()
                    .filter(a -> "OVERDUE".equalsIgnoreCase(a.getDeadlineStatus()))
                    .collect(Collectors.toList());
            case "due_soon" -> filtered = formatted.stream()
                    .filter(a -> "DUE_SOON".equalsIgnoreCase(a.getDeadlineStatus()) || "DUE_TODAY".equalsIgnoreCase(a.getDeadlineStatus()))
                    .collect(Collectors.toList());
        }

        AssignmentSummaryDto summary = new AssignmentSummaryDto(
                formatted.size(),
                (int) formatted.stream().filter(a -> !"COMPLETED".equalsIgnoreCase(a.getDeadlineStatus()) && !"Completed".equalsIgnoreCase(a.getStatus())).count(),
                (int) formatted.stream().filter(a -> "COMPLETED".equalsIgnoreCase(a.getDeadlineStatus()) || "Completed".equalsIgnoreCase(a.getStatus())).count(),
                (int) formatted.stream().filter(a -> "OVERDUE".equalsIgnoreCase(a.getDeadlineStatus())).count(),
                (int) formatted.stream().filter(a -> "DUE_SOON".equalsIgnoreCase(a.getDeadlineStatus()) || "DUE_TODAY".equalsIgnoreCase(a.getDeadlineStatus())).count()
        );

        return new AssignmentListResponse(true, summary, filtered);
    }

    @Transactional(readOnly = true)
    public AssignmentDto getAssignmentById(String userId, String id) {
        Student student = getStudentByUserId(userId);
        Assignment existing = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        verifyOwnership(existing, student.getId());
        return mapToDto(existing);
    }

    @Transactional
    public AssignmentDto createAssignment(String userId, AssignmentCreateRequest req) {
        Student student = getStudentByUserId(userId);

        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }

        String subjectId = resolveSubjectId(req.getSubjectId(), req.getCourse());
        LocalDate dueDate = parseDueDate(req.getDueDate());

        String id = "asg-" + UUID.randomUUID().toString().substring(0, 8);
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setStudentId(student.getId());
        assignment.setSubjectId(subjectId);
        assignment.setTitle(req.getTitle().trim());
        assignment.setDescription(req.getDescription() != null ? req.getDescription().trim() : "");
        assignment.setDueDate(dueDate);
        assignment.setPriority(req.getPriority() != null ? req.getPriority() : "Medium");
        assignment.setStatus("Pending");
        assignment.setEstimatedHours(req.getEstimatedHours() != null
                ? BigDecimal.valueOf(req.getEstimatedHours())
                : new BigDecimal("2.0"));

        Assignment saved = assignmentRepository.save(assignment);
        return mapToDto(saved);
    }

    @Transactional
    public AssignmentDto updateAssignment(String userId, String id, AssignmentUpdateRequest req) {
        Student student = getStudentByUserId(userId);

        Assignment existing = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        verifyOwnership(existing, student.getId());

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            existing.setTitle(req.getTitle().trim());
        }
        if (req.getDescription() != null) {
            existing.setDescription(req.getDescription().trim());
        }
        if (req.getSubjectId() != null && !req.getSubjectId().isBlank()) {
            existing.setSubjectId(req.getSubjectId());
        }
        if (req.getDueDate() != null && !req.getDueDate().isBlank()) {
            existing.setDueDate(parseDueDate(req.getDueDate()));
        }
        if (req.getPriority() != null && !req.getPriority().isBlank()) {
            existing.setPriority(req.getPriority());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            existing.setStatus(req.getStatus());
        }
        if (req.getEstimatedHours() != null) {
            existing.setEstimatedHours(BigDecimal.valueOf(req.getEstimatedHours()));
        }

        Assignment saved = assignmentRepository.save(existing);
        return mapToDto(saved);
    }

    @Transactional
    public AssignmentDeleteResponse deleteAssignment(String userId, String id) {
        Student student = getStudentByUserId(userId);

        Assignment existing = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        verifyOwnership(existing, student.getId());

        assignmentRepository.delete(existing);
        return new AssignmentDeleteResponse(true, "Assignment deleted successfully");
    }

    @Transactional(readOnly = true)
    public AdvisorDataDto runAssignmentAdvisor(String userId) {
        Student student = getStudentByUserId(userId);

        List<Assignment> userAssignments = assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc(student.getId());
        List<AssignmentDto> formatted = userAssignments.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        List<String> exams = examRepository.findByStudentIdOrderByExamDateAsc(student.getId()).stream()
                .map(e -> "- " + e.getTitle() + " on " + e.getExamDate())
                .collect(Collectors.toList());

        List<String> attendance = attendanceRepository.findByStudentId(student.getId()).stream()
                .map(a -> {
                    Subject s = subjectRepository.findById(a.getSubjectId()).orElse(null);
                    String name = s != null ? s.getName() : "Subject";
                    int conducted = a.getClassesConducted();
                    int attended = a.getClassesAttended();
                    double pct = conducted > 0 ? ((double) attended / conducted) * 100.0 : 0.0;
                    return "- " + name + ": " + String.format(Locale.US, "%.1f", pct) + "%";
                })
                .collect(Collectors.toList());

        return advisorService.generatePrioritization(formatted, exams, attendance);
    }

    private AssignmentDto mapToDto(Assignment a) {
        Subject sub = subjectRepository.findById(a.getSubjectId()).orElse(null);
        AssignmentMetrics metrics = calculateAssignmentMetrics(a);

        String course = sub != null ? sub.getCode() : "CS";
        String subjectName = sub != null ? sub.getName() : "Course";
        String dueDateStr = a.getDueDate() != null ? a.getDueDate().toString() : "";
        String createdAtStr = a.getCreatedAt() != null ? a.getCreatedAt().toString() : "";
        String completedAtStr = "Completed".equalsIgnoreCase(a.getStatus()) && a.getUpdatedAt() != null
                ? a.getUpdatedAt().toString()
                : null;

        return new AssignmentDto(
                a.getId(),
                a.getStudentId(),
                a.getSubjectId(),
                course,
                subjectName,
                a.getTitle(),
                a.getDescription() != null ? a.getDescription() : "",
                dueDateStr,
                a.getStatus() != null ? a.getStatus() : "Pending",
                a.getPriority() != null ? a.getPriority() : metrics.getUrgency(),
                a.getEstimatedHours() != null ? a.getEstimatedHours().doubleValue() : 2.0,
                "Assignment",
                createdAtStr,
                completedAtStr,
                metrics
        );
    }

    private void verifyOwnership(Assignment existing, String studentId) {
        boolean isOwner = (existing.getStudentId() != null && existing.getStudentId().equals(studentId))
                || (existing.getStudentId() == null && "std-1".equals(studentId));

        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this assignment");
        }
    }

    private String resolveSubjectId(String subjectId, String course) {
        if (subjectId != null && !subjectId.isBlank()) {
            return subjectId;
        }
        if (course != null && !course.isBlank()) {
            Optional<Subject> sub = subjectRepository.findByCode(course);
            if (sub.isPresent()) {
                return sub.get().getId();
            }
        }
        return "sub-1";
    }

    private LocalDate parseDueDate(String dueDateStr) {
        if (dueDateStr == null || dueDateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dueDateStr.trim());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    private Student getStudentByUserId(String userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required"));
    }
}
