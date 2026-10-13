package com.collegecopilot.studyplanner;

import com.collegecopilot.auth.User;
import com.collegecopilot.auth.UserRepository;
import com.collegecopilot.student.*;
import com.collegecopilot.studyplanner.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class StudyPlannerService {

    private static final Logger log = LoggerFactory.getLogger(StudyPlannerService.class);

    private final StudyPlanRepository studyPlanRepository;
    private final StudySessionRepository studySessionRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final ExamRepository examRepository;
    private final GeminiStudyPlannerService geminiService;

    // Cache recent structured plans by planId in memory (matching Node.js in-memory ai_metadata)
    private final Map<String, StructuredPlanDto> structuredPlanCache = new ConcurrentHashMap<>();

    private static final List<String> VALID_SESSION_STATUSES = List.of(
            "Pending", "In Progress", "Completed", "Skipped"
    );

    public StudyPlannerService(
            StudyPlanRepository studyPlanRepository,
            StudySessionRepository studySessionRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            SubjectRepository subjectRepository,
            AttendanceRepository attendanceRepository,
            AssignmentRepository assignmentRepository,
            ExamRepository examRepository,
            GeminiStudyPlannerService geminiService) {
        this.studyPlanRepository = studyPlanRepository;
        this.studySessionRepository = studySessionRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
        this.attendanceRepository = attendanceRepository;
        this.assignmentRepository = assignmentRepository;
        this.examRepository = examRepository;
        this.geminiService = geminiService;
    }

    @Transactional(readOnly = true)
    public PlannerGetResponse getStudyPlan(String userId) {
        Student student = resolveStudent(userId);
        if (student == null) {
            return new PlannerGetResponse(true, new PlannerDataDto(null, Collections.emptyList(), null));
        }

        Optional<StudyPlan> activePlanOpt = studyPlanRepository.findFirstByStudentIdAndStatus(student.getId(), "Active");
        if (activePlanOpt.isEmpty()) {
            return new PlannerGetResponse(true, new PlannerDataDto(null, Collections.emptyList(), null));
        }

        StudyPlan plan = activePlanOpt.get();
        List<StudySession> sessions = studySessionRepository.findByPlanIdOrderBySessionDateAscStartTimeAsc(plan.getId());
        Map<String, Subject> subjectMap = loadSubjectMap();

        List<StudySessionDto> formattedSessions = sessions.stream()
                .map(s -> mapToSessionDto(s, subjectMap))
                .collect(Collectors.toList());

        StructuredPlanDto structuredPlan = structuredPlanCache.get(plan.getId());
        StudyPlanRecordDto planRecordDto = mapToPlanRecordDto(plan, structuredPlan);

        return new PlannerGetResponse(true, new PlannerDataDto(planRecordDto, formattedSessions, structuredPlan));
    }

    @Transactional
    public PlannerGenerateResponse generateStudyPlan(String userId, PlannerGenerateRequest request) {
        Student student = resolveStudent(userId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only students can generate study plans");
        }

        // 1. Gather student academic context
        StudentPlannerContext context = buildStudentContext(student);

        // 2. Generate structured AI Study Plan
        StructuredPlanDto structuredPlan = geminiService.generateStructuredPlan(context, request);

        // 3. Create and persist new StudyPlan
        String planId = "sp-" + System.currentTimeMillis();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(7);
        String title = (request != null && request.getTitle() != null && !request.getTitle().isBlank())
                ? request.getTitle().trim()
                : "Midterm Exam Sprint 2026";

        double dailyHours = (request != null && request.getAvailableHoursPerDay() != null)
                ? request.getAvailableHoursPerDay()
                : 3.0;
        BigDecimal totalHours = BigDecimal.valueOf(dailyHours * 7).setScale(1, RoundingMode.HALF_UP);

        // Mark previous plans for this student as 'Completed'
        List<StudyPlan> previousPlans = studyPlanRepository.findByStudentId(student.getId());
        for (StudyPlan p : previousPlans) {
            if ("Active".equalsIgnoreCase(p.getStatus())) {
                p.setStatus("Completed");
                studyPlanRepository.save(p);
            }
        }

        StudyPlan newPlan = new StudyPlan(planId, student.getId(), title, startDate, endDate, "Active");
        newPlan.setTotalHours(totalHours);
        studyPlanRepository.save(newPlan);

        // Cache the structured plan metadata
        structuredPlanCache.put(planId, structuredPlan);

        // 4. Convert structured tasks into study sessions
        List<Subject> allSubjects = subjectRepository.findAll();
        List<StudySession> createdSessions = new ArrayList<>();
        List<StudySessionDto> sessionDtos = new ArrayList<>();

        String defaultStartTime = (request != null && request.getStartTime() != null) ? request.getStartTime() : "16:00";
        LocalTime parsedDefaultStartTime = parseLocalTime(defaultStartTime, LocalTime.of(16, 0));

        if (structuredPlan != null && structuredPlan.getDailyPlan() != null) {
            int idxCount = 1;
            for (DailyPlanDto dayObj : structuredPlan.getDailyPlan()) {
                if (dayObj.getTasks() != null) {
                    LocalDate sessionDate = parseLocalDate(dayObj.getDate(), startDate);
                    if (sessionDate.isBefore(startDate)) {
                        sessionDate = startDate;
                    }

                    for (DailyTaskDto task : dayObj.getTasks()) {
                        String taskSubject = task.getSubject() != null ? task.getSubject() : "General Study";
                        Subject subMatch = findSubjectMatch(taskSubject, allSubjects);

                        String subjectId = subMatch != null ? subMatch.getId() : (allSubjects.isEmpty() ? "sub-1" : allSubjects.get(0).getId());
                        String subjectCode = subMatch != null ? subMatch.getCode() : "CS";
                        String subjectName = subMatch != null ? subMatch.getName() : taskSubject;

                        String topic = (task.getActivity() != null && !task.getActivity().isBlank())
                                ? task.getActivity()
                                : "Subject Study & Problem Solving";
                        String reason = (task.getReason() != null && !task.getReason().isBlank())
                                ? task.getReason()
                                : "Curriculum Focus";

                        LocalTime startTime = parseLocalTime(task.getStartTime(), parsedDefaultStartTime);
                        LocalTime endTime = parseLocalTime(task.getEndTime(), startTime.plusHours(2));
                        String priority = task.getPriority() != null ? task.getPriority() : "High";

                        String sessionId = "ss-" + System.currentTimeMillis() + "-" + idxCount++;
                        StudySession session = new StudySession(sessionId, planId, subjectId, topic, sessionDate, startTime, endTime);
                        session.setPriority(priority);
                        session.setStatus("Pending");

                        studySessionRepository.save(session);
                        createdSessions.add(session);

                        StudySessionDto dto = new StudySessionDto(
                                sessionId,
                                planId,
                                subjectId,
                                subjectCode,
                                subjectName,
                                topic,
                                reason,
                                sessionDate.toString(),
                                startTime.toString(),
                                endTime.toString(),
                                priority,
                                "Pending"
                        );
                        sessionDtos.add(dto);
                    }
                }
            }
        }

        StudyPlanRecordDto planRecordDto = mapToPlanRecordDto(newPlan, structuredPlan);

        return new PlannerGenerateResponse(
                true,
                "AI Personalized Study Plan generated successfully",
                new PlannerDataDto(planRecordDto, sessionDtos, structuredPlan)
        );
    }

    @Transactional
    public SessionUpdateResponse updateSessionStatus(String userId, String sessionId, SessionStatusUpdateRequest request) {
        Student student = resolveStudent(userId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only students can update session statuses");
        }

        StudySession session = studySessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        // Ownership check: session must belong to an active or past plan owned by this student
        StudyPlan plan = studyPlanRepository.findById(session.getPlanId())
                .orElse(null);

        if (plan == null || !student.getId().equals(plan.getStudentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this study session");
        }

        String targetStatus = request != null ? request.getStatus() : null;
        if (targetStatus == null || !VALID_SESSION_STATUSES.contains(targetStatus)) {
            String allowed = String.join(", ", VALID_SESSION_STATUSES);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Invalid status '%s'. Allowed statuses: %s", targetStatus, allowed));
        }

        // Safe DB status persistence
        session.setStatus(targetStatus);
        try {
            studySessionRepository.save(session);
        } catch (Exception ex) {
            // In case of MySQL ENUM restriction, map to closest compatible ENUM value
            if ("Skipped".equalsIgnoreCase(targetStatus)) {
                session.setStatus("Missed");
                studySessionRepository.save(session);
            } else if ("In Progress".equalsIgnoreCase(targetStatus)) {
                session.setStatus("Pending");
                studySessionRepository.save(session);
            } else {
                throw ex;
            }
        }

        Map<String, Subject> subjectMap = loadSubjectMap();
        StudySessionDto dto = mapToSessionDto(session, subjectMap);
        // Ensure returning target requested status in DTO
        dto.setStatus(targetStatus);

        return new SessionUpdateResponse(true, dto);
    }

    @Transactional
    public PlannerReplanResponse replanSchedule(String userId) {
        Student student = resolveStudent(userId);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Only students can replan study schedules");
        }

        StudyPlan plan = studyPlanRepository.findFirstByStudentIdAndStatus(student.getId(), "Active")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active study plan to replan"));

        List<StudySession> sessions = studySessionRepository.findByPlanIdOrderBySessionDateAscStartTimeAsc(plan.getId());

        int dayOffset = 1;
        LocalDate today = LocalDate.now();

        for (StudySession s : sessions) {
            if (!"Completed".equalsIgnoreCase(s.getStatus())) {
                s.setSessionDate(today.plusDays(dayOffset));
                s.setStatus("Pending");
                studySessionRepository.save(s);
                dayOffset += 1;
            }
        }

        Map<String, Subject> subjectMap = loadSubjectMap();
        List<StudySessionDto> updatedDtos = sessions.stream()
                .map(s -> mapToSessionDto(s, subjectMap))
                .collect(Collectors.toList());

        return new PlannerReplanResponse(
                true,
                "Replanned remaining sessions successfully! Completed sessions preserved.",
                updatedDtos
        );
    }

    private Student resolveStudent(String userIdOrStudentId) {
        if (userIdOrStudentId == null || userIdOrStudentId.isBlank()) {
            return null;
        }

        // Try direct student id first
        Optional<Student> direct = studentRepository.findById(userIdOrStudentId);
        if (direct.isPresent()) {
            return direct.get();
        }

        // Try user_id mapping
        return studentRepository.findByUserId(userIdOrStudentId).orElse(null);
    }

    private StudentPlannerContext buildStudentContext(Student student) {
        StudentPlannerContext context = new StudentPlannerContext();

        Optional<User> userOpt = userRepository.findById(student.getUserId());
        context.setStudentName(userOpt.map(User::getName).orElse("Alex Mercer"));
        context.setDepartment(student.getDepartment());
        context.setSemester(student.getSemester());
        if (student.getGpa() != null) {
            context.setGpa(student.getGpa().doubleValue());
        }

        List<Subject> subjects = subjectRepository.findAll();
        Map<String, Subject> subjectMap = subjects.stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (s1, s2) -> s1));
        context.setSubjectNames(subjects.stream().map(Subject::getName).collect(Collectors.toList()));

        // Attendance
        List<Attendance> attendanceList = attendanceRepository.findByStudentId(student.getId());
        List<StudentPlannerContext.AttendanceSummary> attSummaries = attendanceList.stream().map(a -> {
            Subject sub = subjectMap.get(a.getSubjectId());
            int conducted = a.getClassesConducted();
            int attended = a.getClassesAttended();
            int pct = conducted > 0 ? (int) Math.round((attended * 100.0) / conducted) : 0;
            String status = pct >= 75 ? "Good Standing" : "Low Attendance Warning";
            return new StudentPlannerContext.AttendanceSummary(
                    sub != null ? sub.getName() : "Subject",
                    sub != null ? sub.getCode() : "CS",
                    conducted,
                    attended,
                    pct,
                    status
            );
        }).collect(Collectors.toList());
        context.setAttendance(attSummaries);

        // Assignments
        List<Assignment> assignments = assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc(student.getId());
        List<StudentPlannerContext.AssignmentSummary> assignmentSummaries = assignments.stream()
                .filter(a -> !"Completed".equalsIgnoreCase(a.getStatus()))
                .map(a -> {
                    Subject sub = subjectMap.get(a.getSubjectId());
                    String course = sub != null ? sub.getName() : "Course";
                    return new StudentPlannerContext.AssignmentSummary(
                            a.getTitle(),
                            course,
                            a.getDueDate() != null ? a.getDueDate().toString() : LocalDate.now().toString(),
                            a.getPriority() != null ? a.getPriority() : "Medium"
                    );
                }).collect(Collectors.toList());
        context.setAssignments(assignmentSummaries);

        // Exams
        List<Exam> exams = examRepository.findByStudentIdOrLegacyOrderByExamDateAsc(student.getId());
        List<StudentPlannerContext.ExamSummary> examSummaries = exams.stream()
                .filter(e -> !"Completed".equalsIgnoreCase(e.getStatus()))
                .map(e -> {
                    Subject sub = subjectMap.get(e.getSubjectId());
                    return new StudentPlannerContext.ExamSummary(
                            e.getTitle(),
                            sub != null ? sub.getName() : "Course",
                            e.getExamDate() != null ? e.getExamDate().toString() : LocalDate.now().toString(),
                            e.getStartTime() != null ? e.getStartTime().toString() : "10:00",
                            e.getLocation() != null ? e.getLocation() : "Campus Hall"
                    );
                }).collect(Collectors.toList());
        context.setExams(examSummaries);

        // Active plan title
        studyPlanRepository.findFirstByStudentIdAndStatus(student.getId(), "Active")
                .ifPresent(p -> context.setActivePlanTitle(p.getTitle()));

        return context;
    }

    private Map<String, Subject> loadSubjectMap() {
        return subjectRepository.findAll().stream()
                .collect(Collectors.toMap(Subject::getId, s -> s, (s1, s2) -> s1));
    }

    private StudySessionDto mapToSessionDto(StudySession s, Map<String, Subject> subjectMap) {
        Subject sub = subjectMap.get(s.getSubjectId());
        String code = sub != null ? sub.getCode() : "CS";
        String name = sub != null ? sub.getName() : "Subject";

        return new StudySessionDto(
                s.getId(),
                s.getPlanId(),
                s.getSubjectId(),
                code,
                name,
                s.getTopic(),
                "Curriculum Focus",
                s.getSessionDate() != null ? s.getSessionDate().toString() : LocalDate.now().toString(),
                s.getStartTime() != null ? s.getStartTime().toString() : "16:00",
                s.getEndTime() != null ? s.getEndTime().toString() : "18:00",
                s.getPriority() != null ? s.getPriority() : "Medium",
                s.getStatus() != null ? s.getStatus() : "Pending"
        );
    }

    private StudyPlanRecordDto mapToPlanRecordDto(StudyPlan plan, StructuredPlanDto structuredPlan) {
        if (plan == null) return null;
        return new StudyPlanRecordDto(
                plan.getId(),
                plan.getStudentId(),
                plan.getTitle(),
                plan.getStartDate() != null ? plan.getStartDate().toString() : null,
                plan.getEndDate() != null ? plan.getEndDate().toString() : null,
                plan.getTotalHours(),
                plan.getStatus(),
                structuredPlan
        );
    }

    private Subject findSubjectMatch(String query, List<Subject> subjects) {
        if (query == null || query.isBlank()) return null;
        String qLower = query.toLowerCase().trim();
        for (Subject sub : subjects) {
            String nameLower = sub.getName().toLowerCase();
            if (nameLower.contains(qLower) || qLower.contains(nameLower)) {
                return sub;
            }
            if (sub.getCode() != null && (sub.getCode().equalsIgnoreCase(qLower) || qLower.contains(sub.getCode().toLowerCase()))) {
                return sub;
            }
        }
        return null;
    }

    private LocalDate parseLocalDate(String str, LocalDate fallback) {
        if (str == null || str.isBlank()) return fallback;
        try {
            return LocalDate.parse(str.trim());
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private LocalTime parseLocalTime(String str, LocalTime fallback) {
        if (str == null || str.isBlank()) return fallback;
        try {
            String clean = str.trim();
            if (clean.length() == 5) {
                return LocalTime.parse(clean);
            }
            return LocalTime.parse(clean);
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
