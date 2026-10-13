package com.collegecopilot.admin;

import com.collegecopilot.admin.dto.*;
import com.collegecopilot.assignment.AssignmentService;
import com.collegecopilot.assignment.dto.AssignmentMetrics;
import com.collegecopilot.attendance.AttendanceService;
import com.collegecopilot.attendance.dto.AttendanceMetrics;
import com.collegecopilot.auth.Role;
import com.collegecopilot.auth.User;
import com.collegecopilot.auth.UserRepository;
import com.collegecopilot.career.*;
import com.collegecopilot.career.SkillItem;
import com.collegecopilot.career.dto.InterviewHistoryDataDto;
import com.collegecopilot.career.dto.SkillGapAnalysisDto;
import com.collegecopilot.career.dto.SkillGapItemDto;
import com.collegecopilot.copilot.ChatSessionRepository;
import com.collegecopilot.exam.ExamService;
import com.collegecopilot.exam.dto.ExamMetrics;
import com.collegecopilot.rag.Document;
import com.collegecopilot.rag.DocumentChunk;
import com.collegecopilot.rag.DocumentChunkRepository;
import com.collegecopilot.rag.DocumentRepository;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.student.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private ExamRepository examRepository;
    @Mock private QuizAttemptRepository quizAttemptRepository;
    @Mock private CareerProfileRepository careerProfileRepository;
    @Mock private ResumeRepository resumeRepository;
    @Mock private InterviewPracticeRepository interviewPracticeRepository;
    @Mock private ChatSessionRepository chatSessionRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentChunkRepository documentChunkRepository;
    @Mock private AnnouncementRepository announcementRepository;
    @Mock private FaqRepository faqRepository;
    @Mock private NotificationRepository notificationRepository;

    @Mock private AttendanceService attendanceService;
    @Mock private AssignmentService assignmentService;
    @Mock private ExamService examService;
    @Mock private CareerService careerService;
    @Mock private GeminiInterviewService geminiInterviewService;
    @Mock private GeminiAdminInsightsService geminiAdminInsightsService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(
                studentRepository,
                userRepository,
                attendanceRepository,
                assignmentRepository,
                examRepository,
                quizAttemptRepository,
                careerProfileRepository,
                resumeRepository,
                interviewPracticeRepository,
                chatSessionRepository,
                subjectRepository,
                documentRepository,
                documentChunkRepository,
                announcementRepository,
                faqRepository,
                notificationRepository,
                attendanceService,
                assignmentService,
                examService,
                careerService,
                geminiInterviewService,
                geminiAdminInsightsService
        );
    }

    @Test
    @DisplayName("getDashboardStats calculates authoritative campus overview deterministically")
    void getDashboardStats_calculatesOverviewMetrics() {
        Student s1 = new Student("std-1", "u-1", "CS101", 3, "Computer Science", new BigDecimal("3.85"));
        when(studentRepository.findAll()).thenReturn(List.of(s1));

        Attendance att1 = new Attendance("att-1", "std-1", "sub-1", 20, 18);
        when(attendanceRepository.findAll()).thenReturn(List.of(att1));

        Assignment asg1 = new Assignment("asg-1", "std-1", "sub-1", "Homework 1", LocalDate.now().plusDays(2), "High", "Pending");
        when(assignmentRepository.findAll()).thenReturn(List.of(asg1));
        when(assignmentService.calculateAssignmentMetrics(asg1))
                .thenReturn(new AssignmentMetrics(2, "DUE_SOON", "HIGH"));

        Exam ex1 = new Exam("ex-1", "std-1", "sub-1", "Midterm", LocalDate.now().plusDays(5), LocalTime.of(10, 0), "Hall A");
        when(examRepository.findAll()).thenReturn(List.of(ex1));
        when(examService.calculateExamMetrics(ex1))
                .thenReturn(new ExamMetrics(5, "UPCOMING", "MEDIUM"));

        QuizAttempt q1 = new QuizAttempt("q-1", "std-1", "sub-1", "Algorithms", 8, 10, new BigDecimal("80.00"));
        when(quizAttemptRepository.findAll()).thenReturn(List.of(q1));

        CareerProfile cp1 = new CareerProfile("cp-1", "std-1", "SWE", "Full Stack Developer", "Tech",
                List.of(new SkillItem("React", "INTERMEDIATE")), List.of(), List.of());
        when(careerProfileRepository.findAll()).thenReturn(List.of(cp1));
        when(careerService.calculateSkillGaps(any(), eq("Full Stack Developer")))
                .thenReturn(new SkillGapAnalysisDto("Full Stack Developer", 7, 1, 0, 6, 14.28,
                        List.of("React"), List.of(), List.of("Docker", "SQL"), List.of()));

        Document doc1 = new Document("doc-1", "Syllabus", "syllabus.pdf", 1024, "u-admin", 2);
        when(documentRepository.findAll()).thenReturn(List.of(doc1));
        DocumentChunk chunk1 = new DocumentChunk("c-1", "doc-1", 0, "Content", 1, "[0.1]", "gemini-embedding-2", 768, "v1");
        when(documentChunkRepository.findAll()).thenReturn(List.of(chunk1));

        AdminDashboardDataDto stats = adminService.getDashboardStats();

        assertThat(stats.getStudents().getTotalStudents()).isEqualTo(1);
        assertThat(stats.getStudents().getActiveStudents()).isEqualTo(1);
        assertThat(stats.getAttendance().getTotalStudentsWithAttendance()).isEqualTo(1);
        assertThat(stats.getAttendance().getAverageAttendance()).isEqualTo(90.0);
        assertThat(stats.getAssignments().getTotalAssignments()).isEqualTo(1);
        assertThat(stats.getAssignments().getPendingAssignments()).isEqualTo(1);
        assertThat(stats.getExams().getUpcomingExams()).isEqualTo(1);
        assertThat(stats.getQuizzes().getTotalQuizAttempts()).isEqualTo(1);
        assertThat(stats.getQuizzes().getAverageQuizScore()).isEqualTo(80.0);
        assertThat(stats.getCareer().getStudentsWithSkillGaps()).isEqualTo(1);
        assertThat(stats.getDocuments().getTotalDocuments()).isEqualTo(1);
        assertThat(stats.getDocuments().getTotalIndexedChunks()).isEqualTo(1);
        assertThat(stats.getDocuments().getIndexingStatus()).isEqualTo("Synchronized");
    }

    private List<Assignment> assignmentsRepositoryMock(List<Assignment> list) {
        when(assignmentRepository.findAll()).thenReturn(list);
        return list;
    }

    @Test
    @DisplayName("getAdminStats returns aggregate counts, announcements, and faqs")
    void getAdminStats_returnsCountsAndRecentItems() {
        when(studentRepository.count()).thenReturn(10L);
        when(documentRepository.count()).thenReturn(5L);
        when(subjectRepository.count()).thenReturn(8L);
        when(assignmentRepository.count()).thenReturn(12L);
        when(examRepository.count()).thenReturn(4L);
        when(chatSessionRepository.count()).thenReturn(20L);

        Announcement anc = new Announcement("anc-1", "Welcome", "Welcome to the new semester", "u-admin");
        when(announcementRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(anc));

        Faq faq = new Faq("faq-1", "What is grading scale?", "4.0 scale", "Academic");
        when(faqRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(faq));

        AdminStatsDataDto stats = adminService.getAdminStats();

        assertThat(stats.getTotalStudents()).isEqualTo(10);
        assertThat(stats.getTotalDocuments()).isEqualTo(5);
        assertThat(stats.getTotalSubjects()).isEqualTo(8);
        assertThat(stats.getTotalAssignments()).isEqualTo(12);
        assertThat(stats.getTotalExams()).isEqualTo(4);
        assertThat(stats.getTotalChatSessions()).isEqualTo(20);
        assertThat(stats.getRecentAnnouncements()).hasSize(1);
        assertThat(stats.getRecentAnnouncements().get(0).getTitle()).isEqualTo("Welcome");
        assertThat(stats.getFaqs()).hasSize(1);
        assertThat(stats.getFaqs().get(0).getQuestion()).isEqualTo("What is grading scale?");
    }

    @Test
    @DisplayName("getStudentsList applies filters and computes risk indicators without exposing secrets")
    void getStudentsList_appliesFiltersAndRiskIndicators() {
        User u1 = new User("u-1", "Jane Doe", "jane@univ.edu", "$2a$12$hashedPasswordSecret", Role.STUDENT);
        Student s1 = new Student("std-1", "u-1", "ST100", 2, "Computer Science", new BigDecimal("3.90"));

        when(studentRepository.findAll()).thenReturn(List.of(s1));
        when(userRepository.findAll()).thenReturn(List.of(u1));

        Attendance att = new Attendance("att-1", "std-1", "sub-1", 40, 26);
        when(attendanceRepository.findAll()).thenReturn(List.of(att));
        when(attendanceService.calculateAttendanceMetrics(40, 26, 75))
                .thenReturn(new AttendanceMetrics(40, 26, 65.0, 75, 0, 4, "BELOW_TARGET", "CRITICAL"));

        Assignment asg = new Assignment("asg-1", "std-1", "sub-1", "Lab 1", LocalDate.now().minusDays(3), "High", "Pending");
        when(assignmentRepository.findAll()).thenReturn(List.of(asg));
        when(assignmentService.calculateAssignmentMetrics(asg))
                .thenReturn(new AssignmentMetrics(-3, "OVERDUE", "CRITICAL"));

        when(examRepository.findAll()).thenReturn(List.of());
        when(quizAttemptRepository.findAll()).thenReturn(List.of());
        when(careerProfileRepository.findAll()).thenReturn(List.of());

        List<AdminStudentSummaryDto> list = adminService.getStudentsList("Jane", "2", "Computer Science", "CRITICAL");

        assertThat(list).hasSize(1);
        AdminStudentSummaryDto summary = list.get(0);
        assertThat(summary.getName()).isEqualTo("Jane Doe");
        assertThat(summary.getEmail()).isEqualTo("jane@univ.edu");
        assertThat(summary.getStudentCode()).isEqualTo("ST100");
        assertThat(summary.getAttendance().getPercentage()).isEqualTo(65.0);
        assertThat(summary.getAttendance().getRiskClassification()).isEqualTo("CRITICAL");
        assertThat(summary.getOverdueAssignmentsCount()).isEqualTo(1);
        assertThat(summary.getRiskIndicators()).contains("ATTENDANCE_CRITICAL", "OVERDUE_ASSIGNMENTS");
    }

    @Test
    @DisplayName("getStudentDetail returns comprehensive drill-down for existing student")
    void getStudentDetail_returnsCompleteDrilldown() {
        Student s1 = new Student("std-1", "u-1", "ST100", 2, "Computer Science", new BigDecimal("3.90"));
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(s1));

        User u1 = new User("u-1", "Jane Doe", "jane@univ.edu", "secret", Role.STUDENT);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(u1));

        Subject sub1 = new Subject("sub-1", "CS101", "Programming", "Dr. Smith", 3, "Computer Science");
        when(subjectRepository.findAll()).thenReturn(List.of(sub1));

        Attendance att = new Attendance("att-1", "std-1", "sub-1", 20, 18);
        when(attendanceRepository.findByStudentId("std-1")).thenReturn(List.of(att));
        when(attendanceService.calculateAttendanceMetrics(20, 18, 75))
                .thenReturn(new AttendanceMetrics(20, 18, 90.0, 75, 2, 0, "ON_TRACK", "SAFE"));

        when(assignmentRepository.findByStudentIdOrderByDueDateAsc("std-1")).thenReturn(List.of());
        when(examRepository.findByStudentIdOrderByExamDateAsc("std-1")).thenReturn(List.of());
        when(quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(List.of());
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.empty());
        when(resumeRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(List.of());
        when(interviewPracticeRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(List.of());
        when(geminiInterviewService.analyzeInterviewWeakAreas(any()))
                .thenReturn(new InterviewHistoryDataDto(List.of(), 0, 0.0, Map.of(), Map.of(), List.of(), List.of()));

        AdminStudentDetailDto detail = adminService.getStudentDetail("std-1");

        assertThat(detail).isNotNull();
        assertThat(detail.getProfile().getName()).isEqualTo("Jane Doe");
        assertThat(detail.getAttendance().getOverall().getPercentage()).isEqualTo(90.0);
        assertThat(detail.getAttendance().getSubjects()).hasSize(1);
    }

    @Test
    @DisplayName("getStudentDetail returns null for non-existent student")
    void getStudentDetail_notFound_returnsNull() {
        when(studentRepository.findById("std-unknown")).thenReturn(Optional.empty());
        when(studentRepository.findByStudentCode("std-unknown")).thenReturn(Optional.empty());

        AdminStudentDetailDto detail = adminService.getStudentDetail("std-unknown");

        assertThat(detail).isNull();
    }

    @Test
    @DisplayName("createSubject saves and returns subject with defaults")
    void createSubject_savesSubject() {
        CreateSubjectRequest req = new CreateSubjectRequest("CS201", "Data Structures", "Staff", 4);
        Subject saved = new Subject("sub-123", "CS201", "Data Structures", "Staff", 4, "Computer Science");
        when(subjectRepository.save(any(Subject.class))).thenReturn(saved);

        AdminSubjectDto res = adminService.createSubject(req);

        assertThat(res.getCode()).isEqualTo("CS201");
        assertThat(res.getName()).isEqualTo("Data Structures");
        assertThat(res.getCredits()).isEqualTo(4);
        verify(subjectRepository).save(any(Subject.class));
    }

    @Test
    @DisplayName("createAnnouncement broadcasts notifications to all enrolled students")
    void createAnnouncement_broadcastsNotifications() {
        Student s1 = new Student("std-1", "u-1", "ST100", 1, "CS", new BigDecimal("3.50"));
        Student s2 = new Student("std-2", "u-2", "ST101", 1, "CS", new BigDecimal("3.70"));
        when(studentRepository.findAll()).thenReturn(List.of(s1, s2));

        Announcement anc = new Announcement("anc-1", "Campus Closed", "Heavy snow warning", "u-admin");
        when(announcementRepository.save(any(Announcement.class))).thenReturn(anc);

        CreateAnnouncementRequest req = new CreateAnnouncementRequest("Campus Closed", "Heavy snow warning");
        UserPrincipal adminPrincipal = new UserPrincipal("u-admin", "Dean", "dean@univ.edu", "ADMIN", null, "adm-1");

        AnnouncementDto res = adminService.createAnnouncement(req, adminPrincipal);

        assertThat(res.getTitle()).isEqualTo("Campus Closed");
        assertThat(res.getPostedBy()).isEqualTo("u-admin");
        verify(announcementRepository).save(any(Announcement.class));
        verify(notificationRepository).saveAll(argThat(iterable -> {
            List<Notification> list = new ArrayList<>();
            iterable.forEach(list::add);
            return list.size() == 2 &&
                    list.stream().allMatch(n -> "announcement".equals(n.getType()) && !n.getIsRead());
        }));
    }

    @Test
    @DisplayName("createFaq creates and persists FAQ entry")
    void createFaq_savesFaq() {
        CreateFaqRequest req = new CreateFaqRequest("How to apply for leave?", "Fill form in portal", "Policy");
        Faq saved = new Faq("faq-10", "How to apply for leave?", "Fill form in portal", "Policy");
        when(faqRepository.save(any(Faq.class))).thenReturn(saved);

        FaqDto res = adminService.createFaq(req);

        assertThat(res.getQuestion()).isEqualTo("How to apply for leave?");
        assertThat(res.getCategory()).isEqualTo("Policy");
        verify(faqRepository).save(any(Faq.class));
    }

    @Test
    @DisplayName("getFaqsList returns ordered FAQs")
    void getFaqsList_returnsFaqs() {
        Faq faq = new Faq("faq-1", "What is grading?", "GPA based", "Academic");
        when(faqRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(faq));

        List<FaqDto> list = adminService.getFaqsList();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo("faq-1");
    }

    @Test
    @DisplayName("updateFaq updates existing FAQ")
    void updateFaq_updatesAndReturnsFaq() {
        Faq existing = new Faq("faq-1", "Old question", "Old answer", "General");
        when(faqRepository.findById("faq-1")).thenReturn(Optional.of(existing));
        when(faqRepository.save(any(Faq.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateFaqRequest req = new CreateFaqRequest("New question", "New answer", "Policy");
        FaqDto updated = adminService.updateFaq("faq-1", req);

        assertThat(updated.getQuestion()).isEqualTo("New question");
        assertThat(updated.getAnswer()).isEqualTo("New answer");
        assertThat(updated.getCategory()).isEqualTo("Policy");
    }

    @Test
    @DisplayName("deleteFaq removes existing FAQ")
    void deleteFaq_removesFaq() {
        Faq existing = new Faq("faq-1", "Old question", "Old answer", "General");
        when(faqRepository.findById("faq-1")).thenReturn(Optional.of(existing));

        adminService.deleteFaq("faq-1");

        verify(faqRepository).delete(existing);
    }
}
