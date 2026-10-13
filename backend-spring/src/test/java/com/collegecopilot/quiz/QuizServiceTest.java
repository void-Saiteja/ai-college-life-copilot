package com.collegecopilot.quiz;

import com.collegecopilot.quiz.dto.*;
import com.collegecopilot.student.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private QuizAttemptRepository quizAttemptRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private GeminiQuizService geminiQuizService;

    @InjectMocks
    private QuizService quizService;

    private Student student1;
    private Student student2;
    private Subject csSubject;
    private List<QuizQuestionDto> sampleQuestions;

    @BeforeEach
    void setUp() {
        student1 = new Student("std-1", "user-1", "2026-CS-001", 4, "Computer Science", new BigDecimal("3.85"));
        student2 = new Student("std-2", "user-2", "2026-CS-002", 4, "Computer Science", new BigDecimal("3.50"));

        csSubject = new Subject("sub-1", "CS101", "Data Structures & Algorithms", "Dr. Smith", 4, "Computer Science");

        sampleQuestions = List.of(
                new QuizQuestionDto(1, "What is a Stack?", List.of("FIFO", "LIFO", "Tree", "Graph"), 1, "Stack is LIFO."),
                new QuizQuestionDto(2, "What is a Queue?", List.of("FIFO", "LIFO", "Tree", "Graph"), 0, "Queue is FIFO.")
        );
    }

    @Test
    @DisplayName("generateQuiz creates and stores quiz, returning sanitized public questions")
    void generateQuiz_success() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(examRepository.findByStudentIdOrLegacyOrderByExamDateAsc("std-1")).thenReturn(List.of());
        when(geminiQuizService.generateQuizQuestions(anyString(), anyString(), anyString(), anyInt(), anyList()))
                .thenReturn(new GeminiQuizService.QuizGenerationResult(sampleQuestions, false));

        QuizGenerateRequest request = new QuizGenerateRequest("CS 201", "Stacks & Queues", "Easy", 2);

        QuizGenerateResponse response = quizService.generateQuiz("std-1", request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertNotNull(response.getData().getQuizId());
        assertEquals("CS 201", response.getData().getSubject());
        assertEquals("Stacks & Queues", response.getData().getTopic());
        assertEquals(2, response.getData().getTotalQuestions());
        assertEquals(2, response.getData().getQuestions().size());

        // Verify correct answers and explanations were stripped from public response
        for (QuizQuestionDto q : response.getData().getQuestions()) {
            assertNull(q.getCorrectAnswer());
            assertNull(q.getExplanation());
        }

        // Verify full question record is stored in active store
        QuizRecord stored = quizService.getActiveQuizForTest(response.getData().getQuizId());
        assertNotNull(stored);
        assertEquals(student1.getId(), stored.getStudentId());
        assertEquals(1, stored.getQuestions().get(0).getCorrectAnswer());
        assertEquals("Stack is LIFO.", stored.getQuestions().get(0).getExplanation());
    }

    @Test
    @DisplayName("generateQuiz throws 401 when student is unauthenticated")
    void generateQuiz_unauthenticated_throws401() {
        when(studentRepository.findById("unknown")).thenReturn(Optional.empty());
        when(studentRepository.findByUserId("unknown")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.generateQuiz("unknown", new QuizGenerateRequest()));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    @DisplayName("submitQuiz evaluates answers correctly and persists QuizAttempt to database")
    void submitQuiz_perfectScore_evaluatesAndPersistsAttempt() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(subjectRepository.findAll()).thenReturn(List.of(csSubject));

        // Inject active quiz
        String quizId = "qz-test-1";
        QuizRecord record = new QuizRecord(quizId, "std-1", "CS 101", "Data Structures", "Medium", 2, sampleQuestions, "2026-10-07");
        quizService.putActiveQuizForTest(quizId, record);

        // Submit both correct answers (Q1 -> 1, Q2 -> 0)
        Map<String, Object> answers = Map.of("1", 1, "2", 0);
        QuizSubmitRequest submitRequest = new QuizSubmitRequest(answers, null);

        QuizSubmitResponse response = quizService.submitQuiz("std-1", quizId, submitRequest);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        QuizSubmissionResultDto data = response.getData();
        assertEquals(2, data.getScore());
        assertEquals(2, data.getTotalQuestions());
        assertEquals(2, data.getCorrectCount());
        assertEquals(0, data.getIncorrectCount());
        assertEquals(0, data.getUnansweredCount());
        assertEquals(100.0, data.getPercentage());
        assertEquals("STRONG", data.getMasteryLevel());
        assertTrue(data.getWeakTopics().isEmpty());

        // Verify QuizAttempt persistence
        verify(quizAttemptRepository, times(1)).save(any(QuizAttempt.class));
        assertEquals("Completed", record.getStatus());
        assertEquals(2, record.getScore());
    }

    @Test
    @DisplayName("submitQuiz handles incorrect and unanswered questions, calculating percentage and weakTopics")
    void submitQuiz_partialAndUnanswered() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(subjectRepository.findAll()).thenReturn(List.of(csSubject));

        String quizId = "qz-test-2";
        QuizRecord record = new QuizRecord(quizId, "std-1", "CS 101", "Trees & Graphs", "Medium", 2, sampleQuestions, "2026-10-07");
        quizService.putActiveQuizForTest(quizId, record);

        // Submit Q1 incorrect (selected 0, correct is 1), Q2 unanswered
        Map<String, Object> answers = Map.of("1", 0);
        QuizSubmitRequest submitRequest = new QuizSubmitRequest(answers, null);

        QuizSubmitResponse response = quizService.submitQuiz("std-1", quizId, submitRequest);

        assertNotNull(response);
        QuizSubmissionResultDto data = response.getData();
        assertEquals(0, data.getScore());
        assertEquals(2, data.getTotalQuestions());
        assertEquals(0, data.getCorrectCount());
        assertEquals(1, data.getIncorrectCount());
        assertEquals(1, data.getUnansweredCount());
        assertEquals(0.0, data.getPercentage());
        assertEquals("WEAK", data.getMasteryLevel());
        assertEquals(List.of("Trees & Graphs"), data.getWeakTopics());

        verify(quizAttemptRepository, times(1)).save(any(QuizAttempt.class));
    }

    @Test
    @DisplayName("submitQuiz rejects cross-student submission with 403")
    void submitQuiz_crossStudent_throws403() {
        when(studentRepository.findById("std-2")).thenReturn(Optional.of(student2));

        String quizId = "qz-student1";
        QuizRecord record = new QuizRecord(quizId, "std-1", "CS 101", "Topic", "Medium", 2, sampleQuestions, "2026-10-07");
        quizService.putActiveQuizForTest(quizId, record);

        QuizSubmitRequest request = new QuizSubmitRequest(Map.of("1", 1), null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.submitQuiz("std-2", quizId, request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You do not own this quiz"));
    }

    @Test
    @DisplayName("submitQuiz throws 404 when quiz is not found and rawQuestions is empty")
    void submitQuiz_missingQuiz_throws404() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));

        QuizSubmitRequest request = new QuizSubmitRequest(Map.of("1", 1), null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.submitQuiz("std-1", "qz-non-existent", request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Quiz record not found"));
    }

    @Test
    @DisplayName("submitQuiz evaluates rawQuestions when provided for backwards compatibility")
    void submitQuiz_rawQuestionsFallback_success() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(subjectRepository.findAll()).thenReturn(List.of(csSubject));
        when(geminiQuizService.validateQuizQuestion(any())).thenReturn(true);

        QuizSubmitRequest request = new QuizSubmitRequest(Map.of("1", 1, "2", 0), sampleQuestions);

        QuizSubmitResponse response = quizService.submitQuiz("std-1", null, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(2, response.getData().getScore());
        verify(quizAttemptRepository, times(1)).save(any(QuizAttempt.class));
    }

    @Test
    @DisplayName("submitQuiz rejects duplicate submission of an already completed quiz with 409 Conflict")
    void submitQuiz_duplicateSubmission_throws409() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));

        String quizId = "qz-already-completed";
        QuizRecord record = new QuizRecord(quizId, "std-1", "CS 101", "Stacks", "Medium", 2, sampleQuestions, "2026-10-07");
        record.setStatus("Completed");
        quizService.putActiveQuizForTest(quizId, record);

        QuizSubmitRequest request = new QuizSubmitRequest(Map.of("1", 1), null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.submitQuiz("std-1", quizId, request));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("already been submitted"));
    }

    @Test
    @DisplayName("submitQuiz safely handles out-of-bounds selected answer indices without errors")
    void submitQuiz_outOfBoundsAnswerIndex_handledSafely() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));

        String quizId = "qz-bounds-test";
        QuizRecord record = new QuizRecord(quizId, "std-1", "CS 101", "Stacks", "Medium", 2, sampleQuestions, "2026-10-07");
        quizService.putActiveQuizForTest(quizId, record);

        QuizSubmitRequest request = new QuizSubmitRequest(Map.of("1", 99, "2", -1), null);

        QuizSubmitResponse response = quizService.submitQuiz("std-1", quizId, request);

        assertNotNull(response);
        assertEquals(0, response.getData().getScore());
        assertEquals(2, response.getData().getUnansweredCount());
        assertNull(response.getData().getResults().get(0).getSelectedAnswer());
    }

    @Test
    @DisplayName("submitQuiz rejects malformed rawQuestions with 400 Bad Request")
    void submitQuiz_malformedRawQuestions_throws400() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(geminiQuizService.validateQuizQuestion(any())).thenReturn(false);

        QuizSubmitRequest request = new QuizSubmitRequest(Map.of("1", 1), sampleQuestions);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.submitQuiz("std-1", null, request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("getQuizzesAndHistory returns student attempts and quiz summaries")
    void getQuizzesAndHistory_success() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));

        QuizAttempt attempt = new QuizAttempt("qa-1", "std-1", "sub-1", "Binary Trees", 5, 5, new BigDecimal("100.00"));
        when(quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(List.of(attempt));
        when(subjectRepository.findById("sub-1")).thenReturn(Optional.of(csSubject));

        QuizRecord record = new QuizRecord("qz-1", "std-1", "CS 101", "Binary Trees", "Medium", 5, sampleQuestions, "2026-10-07");
        quizService.putActiveQuizForTest("qz-1", record);

        QuizHistoryResponse response = quizService.getQuizzesAndHistory("std-1");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(1, response.getData().getAttempts().size());
        assertEquals("Binary Trees", response.getData().getAttempts().get(0).getTopic());
        assertEquals(1, response.getData().getQuizzes().size());
        assertEquals("qz-1", response.getData().getQuizzes().get(0).getId());
    }

    @Test
    @DisplayName("getQuizById sanitizes questions if pending, and includes full answers if completed")
    void getQuizById_sanitizationAndCompleted() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));

        // 1. Pending quiz
        String pendingId = "qz-pending";
        QuizRecord pending = new QuizRecord(pendingId, "std-1", "CS 101", "Trees", "Medium", 2, sampleQuestions, "2026-10-07");
        quizService.putActiveQuizForTest(pendingId, pending);

        QuizDetailResponse pendingResponse = quizService.getQuizById("std-1", pendingId);
        assertNotNull(pendingResponse);
        for (QuizQuestionDto q : pendingResponse.getData().getQuestions()) {
            assertNull(q.getCorrectAnswer());
            assertNull(q.getExplanation());
        }

        // 2. Completed quiz
        String completedId = "qz-completed";
        QuizRecord completed = new QuizRecord(completedId, "std-1", "CS 101", "Trees", "Medium", 2, sampleQuestions, "2026-10-07");
        completed.setStatus("Completed");
        quizService.putActiveQuizForTest(completedId, completed);

        QuizDetailResponse completedResponse = quizService.getQuizById("std-1", completedId);
        assertNotNull(completedResponse);
        assertEquals(1, completedResponse.getData().getQuestions().get(0).getCorrectAnswer());
        assertEquals("Stack is LIFO.", completedResponse.getData().getQuestions().get(0).getExplanation());
    }

    @Test
    @DisplayName("getQuizById throws 403 on cross-student access")
    void getQuizById_crossStudent_throws403() {
        when(studentRepository.findById("std-2")).thenReturn(Optional.of(student2));

        String quizId = "qz-student1";
        QuizRecord record = new QuizRecord(quizId, "std-1", "CS 101", "Topic", "Medium", 2, sampleQuestions, "2026-10-07");
        quizService.putActiveQuizForTest(quizId, record);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.getQuizById("std-2", quizId));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("You do not own this quiz"));
    }

    @Test
    @DisplayName("deleteQuiz deletes owned quiz and attempt, and rejects cross-student deletion")
    void deleteQuiz_behavior() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(studentRepository.findById("std-2")).thenReturn(Optional.of(student2));

        String quizId = "qz-del-1";
        QuizRecord record = new QuizRecord(quizId, "std-1", "CS 101", "Topic", "Medium", 2, sampleQuestions, "2026-10-07");
        quizService.putActiveQuizForTest(quizId, record);

        // Cross-student attempt
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                quizService.deleteQuiz("std-2", quizId));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        // Owner deletes
        QuizDeleteResponse response = quizService.deleteQuiz("std-1", quizId);
        assertTrue(response.isSuccess());
        assertNull(quizService.getActiveQuizForTest(quizId));
    }

    @Test
    @DisplayName("runQuizAdvisor passes student attempts and exams to Gemini advisor service")
    void runQuizAdvisor_success() {
        when(studentRepository.findById("std-1")).thenReturn(Optional.of(student1));
        when(quizAttemptRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(List.of());
        when(examRepository.findByStudentIdOrLegacyOrderByExamDateAsc("std-1")).thenReturn(List.of());

        QuizAdvisorDataDto mockAdvice = new QuizAdvisorDataDto("Summary", "Recommendation", List.of(), List.of(), false);
        when(geminiQuizService.generateAdvisorAdvice(anyList(), anyList())).thenReturn(mockAdvice);

        QuizAdvisorResponse response = quizService.runQuizAdvisor("std-1");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Summary", response.getData().getSummary());
        assertEquals("Recommendation", response.getData().getRecommendation());
    }
}
