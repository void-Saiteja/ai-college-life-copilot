package com.collegecopilot.admin;

import com.collegecopilot.admin.dto.*;
import com.collegecopilot.assignment.AssignmentService;
import com.collegecopilot.assignment.dto.AssignmentMetrics;
import com.collegecopilot.attendance.AttendanceService;
import com.collegecopilot.attendance.dto.AttendanceMetrics;
import com.collegecopilot.auth.User;
import com.collegecopilot.auth.UserRepository;
import com.collegecopilot.career.*;
import com.collegecopilot.career.dto.InterviewHistoryDataDto;
import com.collegecopilot.career.dto.SkillGapAnalysisDto;
import com.collegecopilot.copilot.ChatSessionRepository;
import com.collegecopilot.exam.ExamService;
import com.collegecopilot.exam.dto.ExamMetrics;
import com.collegecopilot.rag.Document;
import com.collegecopilot.rag.DocumentChunk;
import com.collegecopilot.rag.DocumentChunkRepository;
import com.collegecopilot.rag.DocumentRepository;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.student.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssignmentRepository assignmentRepository;
    private final ExamRepository examRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CareerProfileRepository careerProfileRepository;
    private final ResumeRepository resumeRepository;
    private final InterviewPracticeRepository interviewPracticeRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final SubjectRepository subjectRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final AnnouncementRepository announcementRepository;
    private final FaqRepository faqRepository;
    private final NotificationRepository notificationRepository;

    private final AttendanceService attendanceService;
    private final AssignmentService assignmentService;
    private final ExamService examService;
    private final CareerService careerService;
    private final GeminiInterviewService geminiInterviewService;
    private final GeminiAdminInsightsService geminiAdminInsightsService;

    @Autowired
    public AdminService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            AttendanceRepository attendanceRepository,
            AssignmentRepository assignmentRepository,
            ExamRepository examRepository,
            QuizAttemptRepository quizAttemptRepository,
            CareerProfileRepository careerProfileRepository,
            ResumeRepository resumeRepository,
            InterviewPracticeRepository interviewPracticeRepository,
            ChatSessionRepository chatSessionRepository,
            SubjectRepository subjectRepository,
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            AnnouncementRepository announcementRepository,
            FaqRepository faqRepository,
            NotificationRepository notificationRepository,
            AttendanceService attendanceService,
            AssignmentService assignmentService,
            ExamService examService,
            CareerService careerService,
            GeminiInterviewService geminiInterviewService,
            GeminiAdminInsightsService geminiAdminInsightsService) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.assignmentRepository = assignmentRepository;
        this.examRepository = examRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.careerProfileRepository = careerProfileRepository;
        this.resumeRepository = resumeRepository;
        this.interviewPracticeRepository = interviewPracticeRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.subjectRepository = subjectRepository;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.announcementRepository = announcementRepository;
        this.faqRepository = faqRepository;
        this.notificationRepository = notificationRepository;
        this.attendanceService = attendanceService;
        this.assignmentService = assignmentService;
        this.examService = examService;
        this.careerService = careerService;
        this.geminiInterviewService = geminiInterviewService;
        this.geminiAdminInsightsService = geminiAdminInsightsService;
    }

    public AdminDashboardDataDto getDashboardStats() {
        List<Student> students = studentRepository.findAll();
        List<Attendance> attendanceList = attendanceRepository.findAll();
        List<Assignment> assignments = assignmentRepository.findAll();
        List<Exam> exams = examRepository.findAll();
        List<QuizAttempt> quizAttempts = quizAttemptRepository.findAll();
        List<CareerProfile> careerProfiles = careerProfileRepository.findAll();
        List<Document> documents = documentRepository.findAll();
        List<DocumentChunk> documentChunks = documentChunkRepository.findAll();

        // 1. STUDENTS
        int totalStudents = students.size();
        Set<String> activeStudentIds = new HashSet<>();
        attendanceList.forEach(a -> { if (a.getStudentId() != null) activeStudentIds.add(a.getStudentId()); });
        assignments.forEach(a -> { if (a.getStudentId() != null) activeStudentIds.add(a.getStudentId()); });
        exams.forEach(e -> { if (e.getStudentId() != null) activeStudentIds.add(e.getStudentId()); });
        quizAttempts.forEach(q -> { if (q.getStudentId() != null) activeStudentIds.add(q.getStudentId()); });
        int activeStudents = activeStudentIds.size();

        // 2. ATTENDANCE
        Map<String, int[]> studentAttMap = new HashMap<>(); // [conducted, attended]
        attendanceList.forEach(a -> {
            if (a.getStudentId() != null) {
                int[] stats = studentAttMap.computeIfAbsent(a.getStudentId(), k -> new int[]{0, 0});
                stats[0] += (a.getClassesConducted() != null ? a.getClassesConducted() : 0);
                stats[1] += (a.getClassesAttended() != null ? a.getClassesAttended() : 0);
            }
        });

        int studentsWithAttendance = studentAttMap.size();
        double totalAttendanceSum = 0.0;
        int belowTargetCount = 0;
        int atRiskCount = 0;

        for (int[] stats : studentAttMap.values()) {
            double pct = stats[0] > 0 ? ((double) stats[1] / stats[0]) * 100.0 : 0.0;
            totalAttendanceSum += pct;
            if (pct < 75.0) {
                belowTargetCount++;
                atRiskCount++;
            } else if (pct < 80.0) {
                atRiskCount++;
            }
        }

        double averageAttendance = studentsWithAttendance > 0
                ? Math.round((totalAttendanceSum / studentsWithAttendance) * 100.0) / 100.0
                : 0.0;

        // 3. ASSIGNMENTS
        int pendingAssignments = 0;
        int completedAssignments = 0;
        int overdueAssignments = 0;
        int dueSoonAssignments = 0;

        for (Assignment a : assignments) {
            boolean isCompleted = "completed".equalsIgnoreCase(a.getStatus());
            if (isCompleted) {
                completedAssignments++;
            } else {
                pendingAssignments++;
                AssignmentMetrics m = assignmentService.calculateAssignmentMetrics(a);
                if ("OVERDUE".equalsIgnoreCase(m.getDeadlineStatus())) {
                    overdueAssignments++;
                }
                if ("DUE_SOON".equalsIgnoreCase(m.getDeadlineStatus()) || "DUE_TODAY".equalsIgnoreCase(m.getDeadlineStatus())) {
                    dueSoonAssignments++;
                }
            }
        }

        // 4. EXAMS
        int upcomingExams = 0;
        int examsToday = 0;
        int completedExams = 0;

        for (Exam e : exams) {
            ExamMetrics m = examService.calculateExamMetrics(e);
            if ("COMPLETED".equalsIgnoreCase(m.getExamStatus())) {
                completedExams++;
            } else if ("TODAY".equalsIgnoreCase(m.getExamStatus())) {
                examsToday++;
                upcomingExams++;
            } else {
                upcomingExams++;
            }
        }

        // 5. QUIZZES
        int totalQuizAttempts = quizAttempts.size();
        double totalQuizScoreSum = 0.0;
        int weakQuizAttempts = 0;
        Map<String, List<Double>> studentQuizScores = new HashMap<>();

        for (QuizAttempt q : quizAttempts) {
            double pct = q.getPercentage() != null ? q.getPercentage().doubleValue() : 0.0;
            totalQuizScoreSum += pct;
            if (pct < 60.0) {
                weakQuizAttempts++;
            }
            if (q.getStudentId() != null) {
                studentQuizScores.computeIfAbsent(q.getStudentId(), k -> new ArrayList<>()).add(pct);
            }
        }

        double averageQuizScore = totalQuizAttempts > 0
                ? Math.round((totalQuizScoreSum / totalQuizAttempts) * 100.0) / 100.0
                : 0.0;

        int weakStudentsCount = 0;
        for (List<Double> scores : studentQuizScores.values()) {
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            if (avg < 60.0) {
                weakStudentsCount++;
            }
        }

        // 6. CAREER
        int studentsWithCareerProfiles = careerProfiles.size();
        int studentsWithSkillGaps = 0;
        Map<String, Integer> skillGapFrequency = new HashMap<>();

        for (CareerProfile p : careerProfiles) {
            String role = p.getTargetRole() != null ? p.getTargetRole() : "Full Stack Developer";
            SkillGapAnalysisDto gaps = careerService.calculateSkillGaps(p.getSkills(), role);
            if (gaps.getGapCount() > 0) {
                studentsWithSkillGaps++;
            }
            if (gaps.getMissingList() != null) {
                for (String skill : gaps.getMissingList()) {
                    skillGapFrequency.put(skill, skillGapFrequency.getOrDefault(skill, 0) + 1);
                }
            }
        }

        List<AdminDashboardDataDto.SkillGapCount> commonSkillGaps = skillGapFrequency.entrySet().stream()
                .map(e -> new AdminDashboardDataDto.SkillGapCount(e.getKey(), e.getValue()))
                .sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
                .limit(5)
                .collect(Collectors.toList());

        // 7. DOCUMENTS
        int totalDocuments = documents.size();
        int totalIndexedChunks = documentChunks.size();
        String indexingStatus = totalIndexedChunks > 0 ? "Synchronized" : "Pending";

        return new AdminDashboardDataDto(
                new AdminDashboardDataDto.StudentsStats(totalStudents, activeStudents),
                new AdminDashboardDataDto.AttendanceStats(studentsWithAttendance, averageAttendance, belowTargetCount, atRiskCount),
                new AdminDashboardDataDto.AssignmentsStats(assignments.size(), pendingAssignments, completedAssignments, overdueAssignments, dueSoonAssignments),
                new AdminDashboardDataDto.ExamsStats(exams.size(), upcomingExams, examsToday, completedExams),
                new AdminDashboardDataDto.QuizzesStats(totalQuizAttempts, averageQuizScore, weakQuizAttempts, weakStudentsCount),
                new AdminDashboardDataDto.CareerStats(studentsWithCareerProfiles, studentsWithSkillGaps, commonSkillGaps),
                new AdminDashboardDataDto.DocumentsStats(totalDocuments, totalIndexedChunks, "Google Gemini gemini-embedding-2", indexingStatus)
        );
    }

    public AdminStatsDataDto getAdminStats() {
        int totalStudents = (int) studentRepository.count();
        int totalDocuments = (int) documentRepository.count();
        int totalSubjects = (int) subjectRepository.count();
        int totalAssignments = (int) assignmentRepository.count();
        int totalExams = (int) examRepository.count();
        int totalChatSessions = (int) chatSessionRepository.count();

        List<AnnouncementDto> recentAnnouncements = announcementRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(5)
                .map(this::toAnnouncementDto)
                .collect(Collectors.toList());

        List<FaqDto> faqs = faqRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toFaqDto)
                .collect(Collectors.toList());

        return new AdminStatsDataDto(
                totalStudents,
                totalDocuments,
                totalSubjects,
                totalAssignments,
                totalExams,
                totalChatSessions,
                recentAnnouncements,
                faqs
        );
    }

    public List<AdminStudentSummaryDto> getStudentsList(String search, String semester, String department, String attendanceRisk) {
        List<Student> students = studentRepository.findAll();
        List<User> users = userRepository.findAll();
        Map<String, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<Attendance> allAttendance = attendanceRepository.findAll();
        List<Assignment> allAssignments = assignmentRepository.findAll();
        List<Exam> allExams = examRepository.findAll();
        List<QuizAttempt> allQuizzes = quizAttemptRepository.findAll();
        List<CareerProfile> allCareers = careerProfileRepository.findAll();

        Map<String, List<Attendance>> attByStudent = allAttendance.stream()
                .filter(a -> a.getStudentId() != null)
                .collect(Collectors.groupingBy(Attendance::getStudentId));

        Map<String, List<Assignment>> asgByStudent = allAssignments.stream()
                .filter(a -> a.getStudentId() != null)
                .collect(Collectors.groupingBy(Assignment::getStudentId));

        Map<String, List<Exam>> examsByStudent = allExams.stream()
                .filter(e -> e.getStudentId() != null)
                .collect(Collectors.groupingBy(Exam::getStudentId));

        Map<String, List<QuizAttempt>> quizzesByStudent = allQuizzes.stream()
                .filter(q -> q.getStudentId() != null)
                .collect(Collectors.groupingBy(QuizAttempt::getStudentId));

        Map<String, CareerProfile> careerByStudent = allCareers.stream()
                .filter(c -> c.getStudentId() != null)
                .collect(Collectors.toMap(CareerProfile::getStudentId, c -> c, (a, b) -> a));

        List<AdminStudentSummaryDto> list = new ArrayList<>();

        for (Student student : students) {
            User user = userMap.get(student.getUserId());
            String userName = user != null ? user.getName() : "Unknown Student";
            String userEmail = user != null ? user.getEmail() : "N/A";

            // 1. Attendance aggregation
            List<Attendance> sAtt = attByStudent.getOrDefault(student.getId(), List.of());
            int totalConducted = 0;
            int totalAttended = 0;
            for (Attendance a : sAtt) {
                totalConducted += (a.getClassesConducted() != null ? a.getClassesConducted() : 0);
                totalAttended += (a.getClassesAttended() != null ? a.getClassesAttended() : 0);
            }
            AttendanceMetrics attMetrics = attendanceService.calculateAttendanceMetrics(totalConducted, totalAttended, 75);

            // 2. Assignments
            List<Assignment> sAsg = asgByStudent.getOrDefault(student.getId(), List.of());
            int pendingAssignmentsCount = 0;
            int overdueAssignmentsCount = 0;
            for (Assignment a : sAsg) {
                if (!"completed".equalsIgnoreCase(a.getStatus())) {
                    pendingAssignmentsCount++;
                    AssignmentMetrics m = assignmentService.calculateAssignmentMetrics(a);
                    if ("OVERDUE".equalsIgnoreCase(m.getDeadlineStatus())) {
                        overdueAssignmentsCount++;
                    }
                }
            }

            // 3. Exams
            List<Exam> sExams = examsByStudent.getOrDefault(student.getId(), List.of());
            int upcomingExamsCount = 0;
            int examsTodayCount = 0;
            for (Exam e : sExams) {
                ExamMetrics m = examService.calculateExamMetrics(e);
                if (!"COMPLETED".equalsIgnoreCase(m.getExamStatus())) {
                    upcomingExamsCount++;
                    if ("TODAY".equalsIgnoreCase(m.getExamStatus())) {
                        examsTodayCount++;
                    }
                }
            }

            // 4. Quizzes
            List<QuizAttempt> sQuizzes = quizzesByStudent.getOrDefault(student.getId(), List.of());
            int quizAttemptsCount = sQuizzes.size();
            Double averageQuizScore = null;
            if (quizAttemptsCount > 0) {
                double sum = sQuizzes.stream().mapToDouble(q -> q.getPercentage() != null ? q.getPercentage().doubleValue() : 0.0).sum();
                averageQuizScore = Math.round((sum / quizAttemptsCount) * 100.0) / 100.0;
            }

            // 5. Career Profile
            CareerProfile career = careerByStudent.get(student.getId());
            String targetRole = null;
            Double skillCoverage = null;
            int gapCount = 0;
            if (career != null) {
                targetRole = career.getTargetRole() != null ? career.getTargetRole() : "Full Stack Developer";
                SkillGapAnalysisDto gapAnalysis = careerService.calculateSkillGaps(career.getSkills(), targetRole);
                skillCoverage = gapAnalysis.getCoveragePercentage();
                gapCount = gapAnalysis.getGapCount();
            }

            // 6. Risk Indicators
            List<String> riskIndicators = new ArrayList<>();
            if (totalConducted > 0 && "CRITICAL".equalsIgnoreCase(attMetrics.getRiskClassification())) {
                riskIndicators.add("ATTENDANCE_CRITICAL");
            } else if (totalConducted > 0 && "WARNING".equalsIgnoreCase(attMetrics.getRiskClassification())) {
                riskIndicators.add("ATTENDANCE_WARNING");
            }
            if (overdueAssignmentsCount > 0) riskIndicators.add("OVERDUE_ASSIGNMENTS");
            if (examsTodayCount > 0) riskIndicators.add("EXAM_TODAY");
            if (averageQuizScore != null && averageQuizScore < 60.0) riskIndicators.add("WEAK_QUIZ_PERFORMANCE");
            if (career != null && (gapCount >= 4 || (skillCoverage != null && skillCoverage < 50.0))) {
                riskIndicators.add("CAREER_SKILL_GAPS");
            }

            // Security: Omit password, tokens, credentials
            AdminStudentSummaryDto summary = new AdminStudentSummaryDto(
                    student.getId(),
                    user != null ? user.getId() : null,
                    userName,
                    userEmail,
                    student.getStudentCode() != null ? student.getStudentCode() : "N/A",
                    student.getSemester() != null ? student.getSemester() : 1,
                    student.getDepartment() != null ? student.getDepartment() : "Computer Science & Engineering",
                    student.getGpa() != null ? student.getGpa() : new BigDecimal("3.80"),
                    new AdminStudentSummaryDto.AttendanceSummary(
                            totalConducted,
                            totalAttended,
                            attMetrics.getPercentage(),
                            attMetrics.getRiskClassification(),
                            attMetrics.getStatus()
                    ),
                    pendingAssignmentsCount,
                    overdueAssignmentsCount,
                    upcomingExamsCount,
                    quizAttemptsCount,
                    averageQuizScore,
                    targetRole,
                    skillCoverage,
                    riskIndicators
            );

            // Filtering
            if (matchesFilters(summary, search, semester, department, attendanceRisk)) {
                list.add(summary);
            }
        }

        return list;
    }

    private boolean matchesFilters(AdminStudentSummaryDto s, String search, String semester, String department, String attendanceRisk) {
        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            boolean match = (s.getName() != null && s.getName().toLowerCase().contains(q)) ||
                    (s.getEmail() != null && s.getEmail().toLowerCase().contains(q)) ||
                    (s.getStudentCode() != null && s.getStudentCode().toLowerCase().contains(q)) ||
                    (s.getDepartment() != null && s.getDepartment().toLowerCase().contains(q));
            if (!match) return false;
        }

        if (semester != null && !semester.trim().isEmpty()) {
            if (s.getSemester() == null || !String.valueOf(s.getSemester()).equals(semester.trim())) {
                return false;
            }
        }

        if (department != null && !department.trim().isEmpty()) {
            if (s.getDepartment() == null || !s.getDepartment().equalsIgnoreCase(department.trim())) {
                return false;
            }
        }

        if (attendanceRisk != null && !attendanceRisk.trim().isEmpty()) {
            if (s.getAttendance() == null || s.getAttendance().getRiskClassification() == null ||
                    !s.getAttendance().getRiskClassification().equalsIgnoreCase(attendanceRisk.trim())) {
                return false;
            }
        }

        return true;
    }

    public AdminStudentDetailDto getStudentDetail(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return null;
        }

        Optional<Student> studentOpt = studentRepository.findById(studentId);
        if (studentOpt.isEmpty()) {
            studentOpt = studentRepository.findByStudentCode(studentId);
        }
        if (studentOpt.isEmpty()) {
            return null;
        }

        Student student = studentOpt.get();
        User user = student.getUserId() != null ? userRepository.findById(student.getUserId()).orElse(null) : null;
        List<Subject> subjects = subjectRepository.findAll();
        Map<String, Subject> subjectMap = subjects.stream().collect(Collectors.toMap(Subject::getId, s -> s, (a, b) -> a));

        // 1. Profile
        String createdAtStr = user != null && user.getCreatedAt() != null ? user.getCreatedAt().toString() : null;
        AdminStudentDetailDto.ProfileDto profile = new AdminStudentDetailDto.ProfileDto(
                student.getId(),
                user != null ? user.getId() : null,
                student.getStudentCode(),
                user != null ? user.getName() : "Unknown Student",
                user != null ? user.getEmail() : "N/A",
                student.getSemester(),
                student.getDepartment(),
                student.getGpa(),
                createdAtStr
        );

        // 2. Attendance Detail
        List<Attendance> sAtt = attendanceRepository.findByStudentId(student.getId());
        List<AdminStudentDetailDto.SubjectAttendanceDto> subjectAttList = new ArrayList<>();
        int totalConducted = 0;
        int totalAttended = 0;

        for (Attendance a : sAtt) {
            Subject sub = subjectMap.get(a.getSubjectId());
            int c = a.getClassesConducted() != null ? a.getClassesConducted() : 0;
            int at = a.getClassesAttended() != null ? a.getClassesAttended() : 0;
            totalConducted += c;
            totalAttended += at;

            AttendanceMetrics m = attendanceService.calculateAttendanceMetrics(c, at, 75);
            subjectAttList.add(new AdminStudentDetailDto.SubjectAttendanceDto(
                    a.getSubjectId(),
                    sub != null ? sub.getCode() : "CS",
                    sub != null ? sub.getName() : "Subject",
                    c,
                    at,
                    m.getPercentage(),
                    m.getRiskClassification(),
                    m.getStatus(),
                    m.getMaxCanMiss(),
                    m.getMinRequiredToAttend()
            ));
        }

        AttendanceMetrics overallMetrics = attendanceService.calculateAttendanceMetrics(totalConducted, totalAttended, 75);
        AdminStudentDetailDto.AttendanceOverallDto overallAtt = new AdminStudentDetailDto.AttendanceOverallDto(
                totalConducted,
                totalAttended,
                overallMetrics.getPercentage(),
                75,
                overallMetrics.getRiskClassification(),
                overallMetrics.getStatus(),
                overallMetrics.getMaxCanMiss(),
                overallMetrics.getMinRequiredToAttend()
        );
        AdminStudentDetailDto.AttendanceDetailDto attendanceDto = new AdminStudentDetailDto.AttendanceDetailDto(overallAtt, subjectAttList);

        // 3. Assignments Detail
        List<Assignment> sAsg = assignmentRepository.findByStudentIdOrderByDueDateAsc(student.getId());
        List<AdminStudentDetailDto.AssignmentItemDto> asgList = new ArrayList<>();
        int pendingAsg = 0;
        int overdueAsg = 0;

        for (Assignment a : sAsg) {
            Subject sub = subjectMap.get(a.getSubjectId());
            AssignmentMetrics m = assignmentService.calculateAssignmentMetrics(a);
            boolean isCompleted = "completed".equalsIgnoreCase(a.getStatus());
            if (!isCompleted) {
                pendingAsg++;
                if ("OVERDUE".equalsIgnoreCase(m.getDeadlineStatus())) {
                    overdueAsg++;
                }
            }

            asgList.add(new AdminStudentDetailDto.AssignmentItemDto(
                    a.getId(),
                    a.getTitle(),
                    sub != null ? sub.getName() : "Course",
                    a.getDueDate() != null ? a.getDueDate().toString() : null,
                    a.getPriority(),
                    a.getStatus(),
                    m.getDaysRemaining(),
                    m.getDeadlineStatus(),
                    m.getUrgency()
            ));
        }
        AdminStudentDetailDto.AssignmentsDetailDto assignmentsDto = new AdminStudentDetailDto.AssignmentsDetailDto(
                sAsg.size(),
                pendingAsg,
                overdueAsg,
                asgList
        );

        // 4. Exams Detail
        List<Exam> sExams = examRepository.findByStudentIdOrderByExamDateAsc(student.getId());
        List<AdminStudentDetailDto.ExamItemDto> examList = new ArrayList<>();
        int upcomingExams = 0;
        int examsToday = 0;

        for (Exam e : sExams) {
            Subject sub = subjectMap.get(e.getSubjectId());
            ExamMetrics m = examService.calculateExamMetrics(e);
            if (!"COMPLETED".equalsIgnoreCase(m.getExamStatus())) {
                upcomingExams++;
                if ("TODAY".equalsIgnoreCase(m.getExamStatus())) {
                    examsToday++;
                }
            }

            examList.add(new AdminStudentDetailDto.ExamItemDto(
                    e.getId(),
                    e.getTitle(),
                    sub != null ? sub.getName() : "Course",
                    e.getExamDate() != null ? e.getExamDate().toString() : null,
                    e.getStartTime() != null ? e.getStartTime().toString() : "09:00 AM",
                    e.getLocation(),
                    e.getSyllabus(),
                    m.getDaysRemaining(),
                    m.getExamStatus(),
                    m.getUrgency()
            ));
        }
        AdminStudentDetailDto.ExamsDetailDto examsDto = new AdminStudentDetailDto.ExamsDetailDto(
                sExams.size(),
                upcomingExams,
                examsToday,
                examList
        );

        // 5. Quiz Attempts Detail
        List<QuizAttempt> sQuizzes = quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        double quizSum = 0.0;
        List<AdminStudentDetailDto.QuizAttemptItemDto> quizItemList = new ArrayList<>();

        for (QuizAttempt q : sQuizzes) {
            double p = q.getPercentage() != null ? q.getPercentage().doubleValue() : 0.0;
            quizSum += p;
            quizItemList.add(new AdminStudentDetailDto.QuizAttemptItemDto(
                    q.getId(),
                    q.getSubjectId(),
                    q.getTopic(),
                    q.getScore(),
                    q.getTotalQuestions(),
                    q.getPercentage(),
                    q.getCreatedAt() != null ? q.getCreatedAt().toString() : null
            ));
        }
        double avgQuiz = sQuizzes.isEmpty() ? 0.0 : Math.round((quizSum / sQuizzes.size()) * 100.0) / 100.0;
        AdminStudentDetailDto.QuizzesDetailDto quizzesDto = new AdminStudentDetailDto.QuizzesDetailDto(
                sQuizzes.size(),
                avgQuiz,
                quizItemList
        );

        // 6. Career Profile & Skill Gaps
        Optional<CareerProfile> careerOpt = careerProfileRepository.findByStudentId(student.getId());
        AdminStudentDetailDto.CareerDetailDto careerDto = null;
        if (careerOpt.isPresent()) {
            CareerProfile cp = careerOpt.get();
            String role = cp.getTargetRole() != null ? cp.getTargetRole() : "Full Stack Developer";
            SkillGapAnalysisDto gaps = careerService.calculateSkillGaps(cp.getSkills(), role);
            careerDto = new AdminStudentDetailDto.CareerDetailDto(
                    cp.getCareerGoal(),
                    cp.getTargetRole(),
                    cp.getDomain(),
                    cp.getSkills(),
                    gaps
            );
        }

        // 7. Resumes
        List<Resume> sResumes = resumeRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        List<AdminStudentDetailDto.ResumeSummaryDto> resumeList = new ArrayList<>();
        for (Resume r : sResumes) {
            Double coverage = (r.getAnalysis() != null && r.getAnalysis().coverage() != null)
                    ? r.getAnalysis().coverage().coveragePercentage()
                    : null;
            resumeList.add(new AdminStudentDetailDto.ResumeSummaryDto(
                    r.getId(),
                    r.getTitle(),
                    r.getTargetRole(),
                    r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : (r.getCreatedAt() != null ? r.getCreatedAt().toString() : null),
                    coverage
            ));
        }

        // 8. Interview History & Weak Areas
        List<InterviewPractice> sPractices = interviewPracticeRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        InterviewHistoryDataDto weakAnalysis = geminiInterviewService.analyzeInterviewWeakAreas(sPractices);
        List<InterviewPractice> top5History = sPractices.stream().limit(5).collect(Collectors.toList());

        AdminStudentDetailDto.InterviewDetailDto interviewDto = new AdminStudentDetailDto.InterviewDetailDto(
                sPractices.size(),
                weakAnalysis.averageScore(),
                weakAnalysis.weakAreas(),
                top5History
        );

        return new AdminStudentDetailDto(
                profile,
                attendanceDto,
                assignmentsDto,
                examsDto,
                quizzesDto,
                careerDto,
                resumeList,
                interviewDto
        );
    }

    public AdminRisksDataDto getAcademicRisks() {
        List<AdminStudentSummaryDto> students = getStudentsList(null, null, null, null);

        List<AdminRisksDataDto.AttendanceRiskItem> attendanceRisk = new ArrayList<>();
        List<AdminRisksDataDto.AssignmentRiskItem> assignmentRisk = new ArrayList<>();
        List<AdminRisksDataDto.ExamRiskItem> examRisk = new ArrayList<>();
        List<AdminRisksDataDto.QuizRiskItem> quizRisk = new ArrayList<>();
        List<AdminRisksDataDto.CareerGapItem> careerGap = new ArrayList<>();

        for (AdminStudentSummaryDto s : students) {
            // 1. Attendance Risk (< 75% or CRITICAL)
            if (s.getAttendance() != null && s.getAttendance().getConducted() > 0 &&
                    (s.getAttendance().getPercentage() < 75.0 || "CRITICAL".equalsIgnoreCase(s.getAttendance().getRiskClassification()))) {
                attendanceRisk.add(new AdminRisksDataDto.AttendanceRiskItem(
                        s.getId(),
                        s.getName(),
                        s.getDepartment(),
                        s.getSemester(),
                        s.getAttendance().getPercentage(),
                        s.getAttendance().getConducted(),
                        s.getAttendance().getAttended(),
                        s.getAttendance().getRiskClassification()
                ));
            }

            // 2. Assignment Risk (Overdue assignments)
            if (s.getOverdueAssignmentsCount() > 0) {
                assignmentRisk.add(new AdminRisksDataDto.AssignmentRiskItem(
                        s.getId(),
                        s.getName(),
                        s.getDepartment(),
                        s.getOverdueAssignmentsCount(),
                        s.getPendingAssignmentsCount()
                ));
            }

            // 3. Exam Risk (Exam Today or Upcoming)
            if (s.getRiskIndicators() != null &&
                    (s.getRiskIndicators().contains("EXAM_TODAY") || s.getRiskIndicators().contains("EXAM_IMMINENT"))) {
                examRisk.add(new AdminRisksDataDto.ExamRiskItem(
                        s.getId(),
                        s.getName(),
                        s.getDepartment(),
                        s.getUpcomingExamsCount(),
                        s.getRiskIndicators().contains("EXAM_TODAY")
                ));
            }

            // 4. Quiz Risk (Average < 60%)
            if (s.getAverageQuizScore() != null && s.getAverageQuizScore() < 60.0) {
                quizRisk.add(new AdminRisksDataDto.QuizRiskItem(
                        s.getId(),
                        s.getName(),
                        s.getDepartment(),
                        s.getAverageQuizScore(),
                        s.getQuizAttemptsCount()
                ));
            }

            // 5. Career Skill Gap
            if (s.getRiskIndicators() != null && s.getRiskIndicators().contains("CAREER_SKILL_GAPS")) {
                careerGap.add(new AdminRisksDataDto.CareerGapItem(
                        s.getId(),
                        s.getName(),
                        s.getTargetRole(),
                        s.getSkillCoverage()
                ));
            }
        }

        return new AdminRisksDataDto(
                attendanceRisk,
                assignmentRisk,
                examRisk,
                quizRisk,
                careerGap
        );
    }

    public AdminInsightsDataDto getAdminInsights() {
        AdminDashboardDataDto dashboardStats = getDashboardStats();
        AdminRisksDataDto riskData = getAcademicRisks();
        return geminiAdminInsightsService.generateInsights(dashboardStats, riskData);
    }

    public List<AdminDocumentDto> getAdminDocuments() {
        List<Document> documents = documentRepository.findAllByOrderByCreatedAtDesc();
        List<DocumentChunk> chunks = documentChunkRepository.findAll();

        Map<String, List<DocumentChunk>> chunksByDoc = chunks.stream()
                .collect(Collectors.groupingBy(DocumentChunk::getDocumentId));

        List<AdminDocumentDto> list = new ArrayList<>();
        for (Document doc : documents) {
            List<DocumentChunk> docChunks = chunksByDoc.getOrDefault(doc.getId(), List.of());
            boolean hasEmbeddings = !docChunks.isEmpty() && docChunks.stream()
                    .allMatch(c -> c.getVectorEmbedding() != null && !c.getVectorEmbedding().isBlank());

            String status = hasEmbeddings ? "Indexed" : (!docChunks.isEmpty() ? "Partial" : "Pending");
            String createdAtStr = doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null;

            list.add(new AdminDocumentDto(
                    doc.getId(),
                    doc.getTitle(),
                    doc.getFileName(),
                    doc.getFileSize(),
                    doc.getUploadedBy(),
                    1,
                    docChunks.size(),
                    createdAtStr,
                    status,
                    "gemini-embedding-2",
                    "Google Gemini"
            ));
        }
        return list;
    }

    @Transactional
    public AdminSubjectDto createSubject(CreateSubjectRequest request) {
        String id = "sub-" + System.currentTimeMillis();
        int credits = request.getCredits() != null ? request.getCredits() : 3;
        String instructor = (request.getInstructor() != null && !request.getInstructor().isBlank())
                ? request.getInstructor().trim()
                : "Staff";

        Subject subject = new Subject(
                id,
                request.getCode().trim(),
                request.getName().trim(),
                instructor,
                credits,
                "Computer Science"
        );

        Subject saved = subjectRepository.save(subject);
        return new AdminSubjectDto(
                saved.getId(),
                saved.getCode(),
                saved.getName(),
                saved.getInstructor(),
                saved.getCredits(),
                saved.getDepartment()
        );
    }

    @Transactional
    public AnnouncementDto createAnnouncement(CreateAnnouncementRequest request, UserPrincipal principal) {
        String id = "anc-" + System.currentTimeMillis();
        String postedBy = (principal != null && principal.getId() != null) ? principal.getId() : "u-admin-1";

        Announcement announcement = new Announcement(
                id,
                request.getTitle().trim(),
                request.getContent().trim(),
                postedBy
        );

        Announcement saved = announcementRepository.save(announcement);

        // Broadcast announcement notification to all enrolled students
        broadcastAnnouncementNotification(saved);

        return toAnnouncementDto(saved);
    }

    @Transactional
    public FaqDto createFaq(CreateFaqRequest request) {
        String id = "faq-" + System.currentTimeMillis();
        String category = (request.getCategory() != null && !request.getCategory().isBlank())
                ? request.getCategory().trim()
                : "Academic";

        Faq faq = new Faq(
                id,
                request.getQuestion().trim(),
                request.getAnswer().trim(),
                category
        );

        Faq saved = faqRepository.save(faq);
        return toFaqDto(saved);
    }

    @Transactional(readOnly = true)
    public List<FaqDto> getFaqsList() {
        return faqRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toFaqDto)
                .toList();
    }

    @Transactional
    public FaqDto updateFaq(String id, CreateFaqRequest request) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FAQ with ID '" + id + "' not found"));

        faq.setQuestion(request.getQuestion().trim());
        faq.setAnswer(request.getAnswer().trim());
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            faq.setCategory(request.getCategory().trim());
        }
        faq.setUpdatedAt(LocalDateTime.now());
        Faq saved = faqRepository.save(faq);
        return toFaqDto(saved);
    }

    @Transactional
    public void deleteFaq(String id) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FAQ with ID '" + id + "' not found"));
        faqRepository.delete(faq);
    }

    private void broadcastAnnouncementNotification(Announcement announcement) {
        List<Student> students = studentRepository.findAll();
        List<Notification> notifs = new ArrayList<>();

        for (Student student : students) {
            String recipientId = (student.getUserId() != null && !student.getUserId().isBlank())
                    ? student.getUserId()
                    : student.getId();

            String notifId = "notif-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6);
            notifs.add(new Notification(
                    notifId,
                    recipientId,
                    "Campus Announcement: " + announcement.getTitle(),
                    announcement.getContent(),
                    "announcement",
                    false
            ));
        }

        if (!notifs.isEmpty()) {
            notificationRepository.saveAll(notifs);
        }
    }

    private AnnouncementDto toAnnouncementDto(Announcement a) {
        String createdAtStr = a.getCreatedAt() != null ? a.getCreatedAt().toString() : null;
        return new AnnouncementDto(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getPostedBy(),
                createdAtStr
        );
    }

    private FaqDto toFaqDto(Faq f) {
        String createdAtStr = f.getCreatedAt() != null ? f.getCreatedAt().toString() : null;
        String updatedAtStr = f.getUpdatedAt() != null ? f.getUpdatedAt().toString() : null;
        return new FaqDto(
                f.getId(),
                f.getQuestion(),
                f.getAnswer(),
                f.getCategory(),
                createdAtStr,
                updatedAtStr
        );
    }
}
