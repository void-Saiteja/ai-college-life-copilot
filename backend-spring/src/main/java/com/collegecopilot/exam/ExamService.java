package com.collegecopilot.exam;

import com.collegecopilot.exam.dto.*;
import com.collegecopilot.student.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final AssignmentRepository assignmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final GeminiExamAdvisorService advisorService;

    public ExamService(
            ExamRepository examRepository,
            StudentRepository studentRepository,
            SubjectRepository subjectRepository,
            AssignmentRepository assignmentRepository,
            AttendanceRepository attendanceRepository,
            GeminiExamAdvisorService advisorService) {
        this.examRepository = examRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.assignmentRepository = assignmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.advisorService = advisorService;
    }

    public ExamMetrics calculateExamMetrics(Exam exam) {
        return calculateExamMetrics(exam, LocalDate.now());
    }

    public ExamMetrics calculateExamMetrics(Exam exam, LocalDate today) {
        String statusRaw = (exam.getStatus() != null ? exam.getStatus() : "Upcoming").trim();
        boolean isCompleted = "completed".equalsIgnoreCase(statusRaw);

        if (isCompleted) {
            return new ExamMetrics(0, "COMPLETED", "COMPLETED");
        }

        LocalDate examDate = exam.getExamDate();
        if (examDate == null) {
            return new ExamMetrics(999, "UPCOMING", "LOW");
        }

        int daysRemaining = (int) ChronoUnit.DAYS.between(today, examDate);

        if (daysRemaining < 0) {
            return new ExamMetrics(daysRemaining, "COMPLETED", "COMPLETED");
        }

        String examStatus;
        if (daysRemaining == 0) {
            examStatus = "TODAY";
        } else if (daysRemaining <= 3) {
            examStatus = "VERY_SOON";
        } else {
            examStatus = "UPCOMING";
        }

        String urgency;
        if (daysRemaining == 0) {
            urgency = "CRITICAL";
        } else if (daysRemaining <= 3) {
            urgency = "HIGH";
        } else if (daysRemaining <= 7) {
            urgency = "MEDIUM";
        } else {
            urgency = "LOW";
        }

        return new ExamMetrics(daysRemaining, examStatus, urgency);
    }

    @Transactional(readOnly = true)
    public ExamListResponse getExams(String userId, String statusFilter) {
        Student student = getStudentByUserId(userId);
        List<Exam> userExams = examRepository.findByStudentIdOrLegacyOrderByExamDateAsc(student.getId());

        List<ExamDto> formatted = userExams.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        String sLower = statusFilter != null ? statusFilter.toLowerCase().trim() : "all";
        List<ExamDto> filtered = formatted;

        switch (sLower) {
            case "upcoming" -> filtered = formatted.stream()
                    .filter(e -> !"COMPLETED".equalsIgnoreCase(e.getExamStatus()) && !"Completed".equalsIgnoreCase(e.getStatus()))
                    .collect(Collectors.toList());
            case "completed" -> filtered = formatted.stream()
                    .filter(e -> "COMPLETED".equalsIgnoreCase(e.getExamStatus()) || "Completed".equalsIgnoreCase(e.getStatus()))
                    .collect(Collectors.toList());
            case "today" -> filtered = formatted.stream()
                    .filter(e -> "TODAY".equalsIgnoreCase(e.getExamStatus()))
                    .collect(Collectors.toList());
            case "very_soon", "very soon" -> filtered = formatted.stream()
                    .filter(e -> "VERY_SOON".equalsIgnoreCase(e.getExamStatus()) || "TODAY".equalsIgnoreCase(e.getExamStatus()))
                    .collect(Collectors.toList());
        }

        ExamSummaryDto summary = new ExamSummaryDto(
                formatted.size(),
                (int) formatted.stream().filter(e -> !"COMPLETED".equalsIgnoreCase(e.getExamStatus()) && !"Completed".equalsIgnoreCase(e.getStatus())).count(),
                (int) formatted.stream().filter(e -> "TODAY".equalsIgnoreCase(e.getExamStatus())).count(),
                (int) formatted.stream().filter(e -> "VERY_SOON".equalsIgnoreCase(e.getExamStatus())).count(),
                (int) formatted.stream().filter(e -> "HIGH".equalsIgnoreCase(e.getUrgency()) || "CRITICAL".equalsIgnoreCase(e.getUrgency()) || "MEDIUM".equalsIgnoreCase(e.getUrgency())).count(),
                (int) formatted.stream().filter(e -> "COMPLETED".equalsIgnoreCase(e.getExamStatus()) || "Completed".equalsIgnoreCase(e.getStatus())).count()
        );

        return new ExamListResponse(true, summary, filtered);
    }

    @Transactional(readOnly = true)
    public ExamDto getExamById(String userId, String id) {
        Student student = getStudentByUserId(userId);
        Exam existing = examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));

        verifyOwnership(existing, student.getId());
        return mapToDto(existing);
    }

    @Transactional
    public ExamDto createExam(String userId, ExamCreateRequest req) {
        Student student = getStudentByUserId(userId);

        if (req.getTitle() == null || req.getTitle().trim().isEmpty()
                || req.getExamDate() == null || req.getExamDate().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title and exam date are required");
        }

        String subjectId = resolveSubjectId(req.getSubjectId());
        LocalDate examDate = parseExamDate(req.getExamDate());
        LocalTime startTime = parseStartTime(req.getStartTime());

        String id = "exm-" + UUID.randomUUID().toString().substring(0, 8);
        Exam exam = new Exam();
        exam.setId(id);
        exam.setStudentId(student.getId());
        exam.setSubjectId(subjectId);
        exam.setTitle(req.getTitle().trim());
        exam.setExamDate(examDate);
        exam.setStartTime(startTime);
        exam.setLocation(req.getLocation() != null && !req.getLocation().isBlank() ? req.getLocation().trim() : "Main Exam Hall");
        exam.setSyllabus(req.getSyllabus() != null && !req.getSyllabus().isBlank() ? req.getSyllabus().trim() : "Full syllabus");
        exam.setStatus(req.getStatus() != null && !req.getStatus().isBlank() ? req.getStatus().trim() : "Upcoming");

        Exam saved = examRepository.save(exam);
        return mapToDto(saved);
    }

    @Transactional
    public ExamDto updateExam(String userId, String id, ExamUpdateRequest req) {
        Student student = getStudentByUserId(userId);

        Exam existing = examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));

        verifyOwnership(existing, student.getId());

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            existing.setTitle(req.getTitle().trim());
        }
        if (req.getSubjectId() != null && !req.getSubjectId().isBlank()) {
            existing.setSubjectId(req.getSubjectId().trim());
        }
        if (req.getExamDate() != null && !req.getExamDate().isBlank()) {
            existing.setExamDate(parseExamDate(req.getExamDate()));
        }
        if (req.getStartTime() != null && !req.getStartTime().isBlank()) {
            existing.setStartTime(parseStartTime(req.getStartTime()));
        }
        if (req.getLocation() != null) {
            existing.setLocation(req.getLocation().trim());
        }
        if (req.getSyllabus() != null) {
            existing.setSyllabus(req.getSyllabus().trim());
        }
        if (req.getStatus() != null) {
            existing.setStatus(req.getStatus().trim());
        }

        Exam saved = examRepository.save(existing);
        return mapToDto(saved);
    }

    @Transactional
    public ExamDeleteResponse deleteExam(String userId, String id) {
        Student student = getStudentByUserId(userId);

        Exam existing = examRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam not found"));

        verifyOwnership(existing, student.getId());

        examRepository.delete(existing);
        return new ExamDeleteResponse(true, "Exam deleted successfully");
    }

    @Transactional(readOnly = true)
    public ExamAdvisorDataDto runExamAdvisor(String userId) {
        Student student = getStudentByUserId(userId);

        List<Exam> userExams = examRepository.findByStudentIdOrLegacyOrderByExamDateAsc(student.getId());
        List<ExamDto> formattedExams = userExams.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        List<String> pendingAssignments = assignmentRepository.findByStudentId(student.getId()).stream()
                .filter(a -> !"Completed".equalsIgnoreCase(a.getStatus()))
                .map(a -> "- \"" + a.getTitle() + "\" - Due: " + a.getDueDate())
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

        return advisorService.generatePrioritization(formattedExams, pendingAssignments, attendance);
    }

    private ExamDto mapToDto(Exam e) {
        Subject sub = subjectRepository.findById(e.getSubjectId()).orElse(null);
        ExamMetrics metrics = calculateExamMetrics(e);

        String subjectCode = sub != null ? sub.getCode() : "CS";
        String subjectName = sub != null ? sub.getName() : "Subject";
        String examDateStr = e.getExamDate() != null ? e.getExamDate().toString() : "";
        String startTimeStr = e.getStartTime() != null ? formatStartTime(e.getStartTime()) : "09:00 AM";
        String createdAtStr = e.getCreatedAt() != null ? e.getCreatedAt().toString() : "";

        return new ExamDto(
                e.getId(),
                e.getStudentId(),
                e.getSubjectId(),
                subjectCode,
                subjectName,
                e.getTitle(),
                examDateStr,
                startTimeStr,
                e.getLocation() != null ? e.getLocation() : "Main Exam Hall",
                e.getSyllabus() != null ? e.getSyllabus() : "Full syllabus",
                e.getStatus() != null ? e.getStatus() : "Upcoming",
                createdAtStr,
                metrics
        );
    }

    private void verifyOwnership(Exam existing, String studentId) {
        boolean isOwner = (existing.getStudentId() != null && existing.getStudentId().equals(studentId))
                || (existing.getStudentId() == null && "std-1".equals(studentId));

        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this exam record");
        }
    }

    private String resolveSubjectId(String subjectId) {
        if (subjectId != null && !subjectId.isBlank()) {
            return subjectId.trim();
        }
        return "sub-1";
    }

    private LocalDate parseExamDate(String examDateStr) {
        if (examDateStr == null || examDateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(examDateStr.trim());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    private LocalTime parseStartTime(String startTimeStr) {
        if (startTimeStr == null || startTimeStr.isBlank()) {
            return LocalTime.of(9, 0);
        }
        String clean = startTimeStr.trim();
        try {
            return LocalTime.parse(clean);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalTime.parse(clean, DateTimeFormatter.ofPattern("hh:mm a", Locale.US));
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalTime.parse(clean, DateTimeFormatter.ofPattern("h:mm a", Locale.US));
        } catch (DateTimeParseException ignored) {
        }
        return LocalTime.of(9, 0);
    }

    private String formatStartTime(LocalTime time) {
        try {
            return time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.US));
        } catch (Exception e) {
            return time.toString();
        }
    }

    private Student getStudentByUserId(String userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required"));
    }
}
