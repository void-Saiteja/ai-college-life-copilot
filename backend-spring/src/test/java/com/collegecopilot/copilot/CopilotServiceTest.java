package com.collegecopilot.copilot;

import com.collegecopilot.auth.User;
import com.collegecopilot.auth.UserRepository;
import com.collegecopilot.copilot.dto.*;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.student.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CopilotServiceTest {

    @Mock
    private ChatSessionRepository chatSessionRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

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
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private GeminiCopilotService geminiCopilotService;

    @InjectMocks
    private CopilotService copilotService;

    private UserPrincipal studentPrincipalA;
    private UserPrincipal studentPrincipalB;
    private Student studentA;
    private Student studentB;
    private User userA;

    @BeforeEach
    void setUp() {
        studentPrincipalA = new UserPrincipal("user-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);
        studentPrincipalB = new UserPrincipal("user-2", "Bob Smith", "bob@student.edu", "STUDENT", "std-2", null);

        studentA = new Student("std-1", "user-1", "CS-2026-001", 4, "Computer Science & Engineering", new BigDecimal("3.85"));
        studentB = new Student("std-2", "user-2", "ME-2026-002", 2, "Mechanical Engineering", new BigDecimal("3.50"));

        userA = new User("user-1", "Alex Mercer", "alex@student.edu", "password", com.collegecopilot.auth.Role.STUDENT);
    }

    @Test
    @DisplayName("getChatSessions returns sessions for authenticated student")
    void getChatSessions_returnsStudentSessions() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(studentA));

        ChatSession s1 = new ChatSession("cs-1", "std-1", "Exam Prep");
        ChatSession s2 = new ChatSession("cs-2", "std-1", "Attendance Check");
        when(chatSessionRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(List.of(s1, s2));

        ChatSessionListResponse response = copilotService.getChatSessions(studentPrincipalA);

        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().size());
        assertEquals("Exam Prep", response.getData().get(0).getTitle());
    }

    @Test
    @DisplayName("getSessionMessages returns messages when student owns session")
    void getSessionMessages_ownedSession_returnsMessages() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(studentA));

        ChatSession session = new ChatSession("cs-1", "std-1", "Course Advice");
        when(chatSessionRepository.findById("cs-1")).thenReturn(Optional.of(session));

        ChatMessage m1 = new ChatMessage("cm-1", "cs-1", "user", "What is DSA?");
        ChatMessage m2 = new ChatMessage("cm-2", "cs-1", "assistant", "DSA stands for Data Structures and Algorithms.");
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("cs-1")).thenReturn(List.of(m1, m2));

        ChatMessageListResponse response = copilotService.getSessionMessages("cs-1", studentPrincipalA);

        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().size());
        assertEquals("user", response.getData().get(0).getSender());
        assertEquals("assistant", response.getData().get(1).getSender());
    }

    @Test
    @DisplayName("getSessionMessages throws 403 when accessed by another student")
    void getSessionMessages_crossStudentAccess_throws403() {
        when(studentRepository.findById("std-2")).thenReturn(Optional.of(studentB));

        ChatSession session = new ChatSession("cs-1", "std-1", "Private Session");
        when(chatSessionRepository.findById("cs-1")).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                copilotService.getSessionMessages("cs-1", studentPrincipalB));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Access denied: You do not own this chat session"));
    }

    @Test
    @DisplayName("getSessionMessages throws 404 when session does not exist")
    void getSessionMessages_notFound_throws404() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(studentA));
        when(chatSessionRepository.findById("cs-nonexistent")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                copilotService.getSessionMessages("cs-nonexistent", studentPrincipalA));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("postChatMessage creates new session and stores both user and assistant messages")
    void postChatMessage_newSession_createsSessionAndMessages() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(studentA));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userA));
        when(subjectRepository.findAll()).thenReturn(Collections.emptyList());
        when(attendanceRepository.findByStudentId("std-1")).thenReturn(Collections.emptyList());
        when(assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc("std-1")).thenReturn(Collections.emptyList());
        when(examRepository.findByStudentIdOrLegacyOrderByExamDateAsc("std-1")).thenReturn(Collections.emptyList());
        when(studyPlanRepository.findFirstByStudentIdAndStatus("std-1", "Active")).thenReturn(Optional.empty());
        when(quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(Collections.emptyList());

        when(geminiCopilotService.generateCopilotReply(any(), any(), any()))
                .thenReturn(new GeminiCopilotService.GeminiResponse(true, "I can help you manage your college life!", false, null));

        ChatRequest request = new ChatRequest(null, "Hello, how can you help me?");
        ChatResponse response = copilotService.postChatMessage(request, studentPrincipalA);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData().getSessionId());
        assertEquals("user", response.getData().getUserMessage().getSender());
        assertEquals("Hello, how can you help me?", response.getData().getUserMessage().getContent());
        assertEquals("assistant", response.getData().getAssistantMessage().getSender());
        assertEquals("I can help you manage your college life!", response.getData().getAssistantMessage().getContent());

        // Verify session was saved
        verify(chatSessionRepository, times(1)).save(any(ChatSession.class));
        // Verify user and assistant messages were saved
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("postChatMessage returns simple greeting immediately without invoking Gemini")
    void postChatMessage_simpleGreeting_returnsInstantGreetingWithoutCallingGemini() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(studentA));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userA));

        ChatRequest request = new ChatRequest(null, "hi");
        ChatResponse response = copilotService.postChatMessage(request, studentPrincipalA);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData().getSessionId());
        assertEquals("user", response.getData().getUserMessage().getSender());
        assertEquals("hi", response.getData().getUserMessage().getContent());
        assertEquals("assistant", response.getData().getAssistantMessage().getSender());
        assertTrue(response.getData().getAssistantMessage().getContent().contains("Hello Alex!"));
        assertTrue(response.getData().getAssistantMessage().getContent().contains("AI College Life Copilot"));

        // Verify Gemini was NEVER invoked
        verify(geminiCopilotService, never()).generateCopilotReply(any(), any(), any());
        // Verify user and assistant messages were saved
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("postChatMessage successfully stores and returns fallback response when Gemini is unavailable")
    void postChatMessage_geminiUnavailable_storesAndReturnsFallbackResponse() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(studentA));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userA));
        when(subjectRepository.findAll()).thenReturn(Collections.emptyList());
        when(attendanceRepository.findByStudentId("std-1")).thenReturn(Collections.emptyList());
        when(assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc("std-1")).thenReturn(Collections.emptyList());
        when(examRepository.findByStudentIdOrLegacyOrderByExamDateAsc("std-1")).thenReturn(Collections.emptyList());
        when(studyPlanRepository.findFirstByStudentIdAndStatus("std-1", "Active")).thenReturn(Optional.empty());
        when(quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(Collections.emptyList());

        String fallbackText = "📋 **Academic Copilot (Offline Mode)**\n\n- Cumulative GPA: 3.85";
        when(geminiCopilotService.generateCopilotReply(any(), any(), any()))
                .thenReturn(new GeminiCopilotService.GeminiResponse(true, fallbackText, false, "Service unavailable", true));

        ChatRequest request = new ChatRequest(null, "What is my GPA?");
        ChatResponse response = copilotService.postChatMessage(request, studentPrincipalA);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData().getSessionId());
        assertEquals("assistant", response.getData().getAssistantMessage().getSender());
        assertEquals(fallbackText, response.getData().getAssistantMessage().getContent());
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("postChatMessage regression test: real GeminiCopilotService with 503 high demand returns fallback without StackOverflowError")
    void postChatMessage_simulatedGemini503_returnsFallbackWithoutStackOverflowError() throws Exception {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(studentA));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userA));
        when(subjectRepository.findAll()).thenReturn(Collections.emptyList());
        when(attendanceRepository.findByStudentId("std-1")).thenReturn(Collections.emptyList());
        when(assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc("std-1")).thenReturn(Collections.emptyList());
        when(examRepository.findByStudentIdOrLegacyOrderByExamDateAsc("std-1")).thenReturn(Collections.emptyList());
        when(studyPlanRepository.findFirstByStudentIdAndStatus("std-1", "Active")).thenReturn(Optional.empty());
        when(quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(Collections.emptyList());

        java.net.http.HttpClient mockHttp = mock(java.net.http.HttpClient.class);
        @SuppressWarnings("unchecked")
        java.net.http.HttpResponse<String> mockResp = mock(java.net.http.HttpResponse.class);
        when(mockResp.statusCode()).thenReturn(503);
        when(mockResp.body()).thenReturn("{\"error\":{\"code\":503,\"message\":\"This model is currently experiencing high demand.\",\"status\":\"UNAVAILABLE\"}}");
        doReturn(mockResp).when(mockHttp).send(any(), any());

        GeminiCopilotService realGeminiService = new GeminiCopilotService("valid_gemini_api_key_123", mockHttp);
        CopilotService realCopilotService = new CopilotService(
                chatSessionRepository,
                chatMessageRepository,
                studentRepository,
                userRepository,
                subjectRepository,
                attendanceRepository,
                assignmentRepository,
                examRepository,
                studyPlanRepository,
                quizAttemptRepository,
                realGeminiService
        );

        ChatRequest request = new ChatRequest(null, "Based on that GPA, am I in good academic standing?");
        ChatResponse response = realCopilotService.postChatMessage(request, studentPrincipalA);

        assertTrue(response.isSuccess());
        assertNotNull(response.getData().getSessionId());
        assertEquals("assistant", response.getData().getAssistantMessage().getSender());
        String content = response.getData().getAssistantMessage().getContent();
        assertTrue(content.contains("Academic Copilot (Offline Mode)"));
        assertTrue(content.contains("3.85"));
        assertTrue(content.contains("Dean's List / Excellent Academic Standing"));
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("postChatMessage throws 400 when message is null or blank")
    void postChatMessage_blankMessage_throws400() {
        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class, () ->
                copilotService.postChatMessage(new ChatRequest(null, null), studentPrincipalA));
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatusCode());

        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () ->
                copilotService.postChatMessage(new ChatRequest(null, "   "), studentPrincipalA));
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatusCode());
    }

    @Test
    @DisplayName("postChatMessage throws 400 when message exceeds 4000 characters")
    void postChatMessage_exceedsLength_throws400() {
        String longMsg = "A".repeat(4001);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                copilotService.postChatMessage(new ChatRequest(null, longMsg), studentPrincipalA));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("4000 characters"));
    }

    @Test
    @DisplayName("deleteChatSession deletes messages and session when owned by student")
    void deleteChatSession_owned_deletesSuccessfully() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(studentA));
        ChatSession session = new ChatSession("cs-1", "std-1", "To Delete");
        when(chatSessionRepository.findById("cs-1")).thenReturn(Optional.of(session));

        ChatDeleteResponse response = copilotService.deleteChatSession("cs-1", studentPrincipalA);

        assertTrue(response.isSuccess());
        assertEquals("Chat conversation deleted", response.getMessage());
        verify(chatMessageRepository, times(1)).deleteBySessionId("cs-1");
        verify(chatSessionRepository, times(1)).delete(session);
    }

    @Test
    @DisplayName("deleteChatSession throws 403 when session is owned by another student")
    void deleteChatSession_crossStudent_throws403() {
        when(studentRepository.findById("std-2")).thenReturn(Optional.of(studentB));
        ChatSession session = new ChatSession("cs-1", "std-1", "Student A's session");
        when(chatSessionRepository.findById("cs-1")).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                copilotService.deleteChatSession("cs-1", studentPrincipalB));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(chatSessionRepository, never()).delete(any());
        verify(chatMessageRepository, never()).deleteBySessionId(any());
    }

    @Test
    @DisplayName("buildStudentContext aggregates attendance, assignments, and exams strictly for requested student")
    void buildStudentContext_aggregatesRealData() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userA));

        Subject sub1 = new Subject("sub-1", "CS 201", "Data Structures", "Dr. Alan", 4, "CSE");
        when(subjectRepository.findAll()).thenReturn(List.of(sub1));

        Attendance att = new Attendance("att-1", "std-1", "sub-1", 10, 8);
        when(attendanceRepository.findByStudentId("std-1")).thenReturn(List.of(att));

        Assignment asg = new Assignment("asg-1", "std-1", "sub-1", "Tree Traversal", LocalDate.now().plusDays(2), "High", "Pending");
        when(assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc("std-1")).thenReturn(List.of(asg));

        Exam exam = new Exam("ex-1", "std-1", "sub-1", "DSA Midterm", LocalDate.now().plusDays(5), LocalTime.of(10, 0), "Hall B");
        when(examRepository.findByStudentIdOrLegacyOrderByExamDateAsc("std-1")).thenReturn(List.of(exam));

        StudyPlan plan = new StudyPlan("sp-1", "std-1", "Midterm Sprint", LocalDate.now(), LocalDate.now().plusDays(7), "Active");
        when(studyPlanRepository.findFirstByStudentIdAndStatus("std-1", "Active")).thenReturn(Optional.of(plan));

        QuizAttempt qa = new QuizAttempt("qa-1", "std-1", "sub-1", "Binary Trees", 4, 5, new BigDecimal("80.0"));
        when(quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(List.of(qa));

        StudentCopilotContext context = copilotService.buildStudentContext(studentA);

        assertEquals("Alex Mercer", context.getProfile().getName());
        assertEquals("std-1", context.getProfile().getId());
        assertEquals(1, context.getAttendance().size());
        assertEquals(80, context.getAttendance().get(0).getPercentage());
        assertEquals(1, context.getUpcomingAssignments().size());
        assertEquals("Tree Traversal", context.getUpcomingAssignments().get(0).getTitle());
        assertEquals(1, context.getUpcomingExams().size());
        assertEquals("DSA Midterm", context.getUpcomingExams().get(0).getTitle());
        assertEquals("Midterm Sprint", context.getStudyPlan().getTitle());
        assertEquals(1, context.getQuizHistory().getTotalAttempts());
    }

    @Test
    @DisplayName("postChatMessage excludes failed assistant error messages and security notices from history")
    void postChatMessage_excludesErrorMessagesFromHistory() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(studentA));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userA));
        when(subjectRepository.findAll()).thenReturn(Collections.emptyList());
        when(attendanceRepository.findByStudentId("std-1")).thenReturn(Collections.emptyList());
        when(assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc("std-1")).thenReturn(Collections.emptyList());
        when(examRepository.findByStudentIdOrLegacyOrderByExamDateAsc("std-1")).thenReturn(Collections.emptyList());
        when(studyPlanRepository.findFirstByStudentIdAndStatus("std-1", "Active")).thenReturn(Optional.empty());
        when(quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(Collections.emptyList());

        ChatSession session = new ChatSession("cs-1", "std-1", "Existing Session");
        when(chatSessionRepository.findById("cs-1")).thenReturn(Optional.of(session));

        ChatMessage validUser1 = new ChatMessage("cm-1", "cs-1", "user", "What is my GPA?");
        ChatMessage errorMsg1 = new ChatMessage("cm-2", "cs-1", "assistant", "⚠️ **Gemini AI Service Error**: Unable to connect to Google Gemini API.");
        ChatMessage errorMsg2 = new ChatMessage("cm-3", "cs-1", "assistant", "   ⚠️ **Gemini AI Service Error**: Service unavailable.");
        ChatMessage errorMsg3 = new ChatMessage("cm-4", "cs-1", "assistant", "?? **Gemini AI Service Error**: High demand.");
        ChatMessage errorMsg4 = new ChatMessage("cm-7", "cs-1", "assistant", "â ï¸ **Gemini AI Service Error**: Connection timed out.");
        ChatMessage securityMsg = new ChatMessage("cm-5", "cs-1", "assistant", "🛡️ **Institutional Security Notice**: Unauthorized access.");
        ChatMessage validAssistant = new ChatMessage("cm-6", "cs-1", "assistant", "Your GPA is 3.85.");

        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc("cs-1"))
                .thenReturn(List.of(validUser1, errorMsg1, errorMsg2, errorMsg3, errorMsg4, securityMsg, validAssistant));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessageDto>> historyCaptor = ArgumentCaptor.forClass(List.class);
        when(geminiCopilotService.generateCopilotReply(any(), any(), historyCaptor.capture()))
                .thenReturn(new GeminiCopilotService.GeminiResponse(true, "You are in good academic standing.", false, null));

        ChatRequest request = new ChatRequest("cs-1", "Am I in good standing?");
        ChatResponse response = copilotService.postChatMessage(request, studentPrincipalA);

        assertTrue(response.isSuccess());
        List<ChatMessageDto> capturedHistory = historyCaptor.getValue();
        assertEquals(2, capturedHistory.size(), "Only clean messages should be included in prompt history");
        assertEquals("What is my GPA?", capturedHistory.get(0).getContent());
        assertEquals("Your GPA is 3.85.", capturedHistory.get(1).getContent());
    }

    @Test
    @DisplayName("postChatMessage populates context with authenticated student's actual attended and conducted class counts")
    void postChatMessage_passesActualStudentAttendanceCounts() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(studentA));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(userA));

        Subject sub1 = new Subject("sub-1", "CS201", "Data Structures", "Prof A", 4, "CSE");
        Subject sub2 = new Subject("sub-2", "CS202", "Algorithms", "Prof B", 4, "CSE");
        Subject sub3 = new Subject("sub-3", "CS203", "Computer Networks", "Prof C", 3, "CSE");
        Subject sub4 = new Subject("sub-4", "CS204", "Database Systems", "Prof D", 3, "CSE");
        when(subjectRepository.findAll()).thenReturn(List.of(sub1, sub2, sub3, sub4));

        Attendance att1 = new Attendance("att-1", "std-1", "sub-1", 28, 25);
        Attendance att2 = new Attendance("att-2", "std-1", "sub-2", 20, 18);
        Attendance att3 = new Attendance("att-3", "std-1", "sub-3", 24, 22);
        Attendance att4 = new Attendance("att-4", "std-1", "sub-4", 16, 14);
        when(attendanceRepository.findByStudentId("std-1")).thenReturn(List.of(att1, att2, att3, att4));

        when(assignmentRepository.findByStudentIdOrLegacyOrderByDueDateAsc("std-1")).thenReturn(Collections.emptyList());
        when(examRepository.findByStudentIdOrLegacyOrderByExamDateAsc("std-1")).thenReturn(Collections.emptyList());
        when(studyPlanRepository.findFirstByStudentIdAndStatus("std-1", "Active")).thenReturn(Optional.empty());
        when(quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(Collections.emptyList());

        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<StudentCopilotContext> contextCaptor = ArgumentCaptor.forClass(StudentCopilotContext.class);
        when(geminiCopilotService.generateCopilotReply(anyString(), contextCaptor.capture(), any()))
                .thenReturn(new GeminiCopilotService.GeminiResponse(true, "Calculated response", false, null, true));

        String prompt = "My attendance requirement is 75%. For each subject, how many more classes can I miss while keeping my attendance at or above 75%?";
        ChatRequest request = new ChatRequest(null, prompt);
        ChatResponse response = copilotService.postChatMessage(request, studentPrincipalA);

        assertTrue(response.isSuccess());
        StudentCopilotContext captured = contextCaptor.getValue();
        assertNotNull(captured);
        assertEquals(4, captured.getAttendance().size());

        StudentCopilotContext.AttendanceItem item1 = captured.getAttendance().get(0);
        assertEquals("Data Structures", item1.getSubject());
        assertEquals(28, item1.getConducted());
        assertEquals(25, item1.getAttended());

        StudentCopilotContext.AttendanceItem item2 = captured.getAttendance().get(1);
        assertEquals("Algorithms", item2.getSubject());
        assertEquals(20, item2.getConducted());
        assertEquals(18, item2.getAttended());

        StudentCopilotContext.AttendanceItem item3 = captured.getAttendance().get(2);
        assertEquals("Computer Networks", item3.getSubject());
        assertEquals(24, item3.getConducted());
        assertEquals(22, item3.getAttended());

        StudentCopilotContext.AttendanceItem item4 = captured.getAttendance().get(3);
        assertEquals("Database Systems", item4.getSubject());
        assertEquals(16, item4.getConducted());
        assertEquals(14, item4.getAttended());
    }
}
