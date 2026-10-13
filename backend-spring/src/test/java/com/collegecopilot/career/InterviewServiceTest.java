package com.collegecopilot.career;

import com.collegecopilot.career.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock
    private InterviewQuestionRepository questionRepository;

    @Mock
    private InterviewPracticeRepository practiceRepository;

    @Mock
    private CareerProfileRepository careerProfileRepository;

    @Mock
    private CareerService careerService;

    @Mock
    private GeminiInterviewService geminiInterviewService;

    @InjectMocks
    private InterviewService interviewService;

    private UserPrincipal principalA;
    private UserPrincipal principalB;

    @BeforeEach
    void setUp() {
        principalA = new UserPrincipal("u-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);
        principalB = new UserPrincipal("u-2", "Bob Smith", "bob@student.edu", "STUDENT", "std-2", null);
    }

    @Test
    @DisplayName("generateQuestion creates and stores interview question with 201 Created")
    void testGenerateQuestionSuccess() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");
        when(careerProfileRepository.findByStudentId("std-1")).thenReturn(Optional.empty());
        when(careerService.calculateSkillGaps(any(), eq("Full Stack Developer")))
                .thenReturn(new SkillGapAnalysisDto("Full Stack Developer", 7, 0, 0, 7, 0.0, List.of(), List.of(), List.of(), List.of()));

        GeminiInterviewService.GeneratedQuestionResult genResult = new GeminiInterviewService.GeneratedQuestionResult(
                "Explain the event loop in JavaScript", "TECHNICAL", "MEDIUM", "JavaScript", "Mention microtasks", false
        );
        when(geminiInterviewService.generateInterviewQuestion(eq("Full Stack Developer"), eq("TECHNICAL"), eq("MEDIUM"), eq("JavaScript"), any(), any()))
                .thenReturn(genResult);
        when(questionRepository.save(any(InterviewQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewQuestionGenerateRequest req = new InterviewQuestionGenerateRequest("Full Stack Developer", "TECHNICAL", "MEDIUM", "JavaScript");
        InterviewQuestionResponse response = interviewService.generateQuestion(req, principalA);

        assertTrue(response.success());
        assertNotNull(response.data());
        assertEquals("Explain the event loop in JavaScript", response.data().question());
        assertEquals("TECHNICAL", response.data().category());
        assertEquals("JavaScript", response.data().targetSkill());
        verify(questionRepository, times(1)).save(any(InterviewQuestion.class));
    }

    @Test
    @DisplayName("generateQuestion throws 400 for invalid interview category")
    void testGenerateQuestionInvalidCategory() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");

        InterviewQuestionGenerateRequest req = new InterviewQuestionGenerateRequest("Full Stack Developer", "INVALID_CAT", "MEDIUM", "Java");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interviewService.generateQuestion(req, principalA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid interview category 'INVALID_CAT'"));
    }

    @Test
    @DisplayName("generateQuestion throws 400 for invalid difficulty")
    void testGenerateQuestionInvalidDifficulty() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");

        InterviewQuestionGenerateRequest req = new InterviewQuestionGenerateRequest("Full Stack Developer", "TECHNICAL", "EXTREME", "Java");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interviewService.generateQuestion(req, principalA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid difficulty 'EXTREME'"));
    }

    @Test
    @DisplayName("generateQuestion throws 400 for invalid target role")
    void testGenerateQuestionInvalidTargetRole() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");

        InterviewQuestionGenerateRequest req = new InterviewQuestionGenerateRequest("Astronaut", "TECHNICAL", "MEDIUM", "Java");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interviewService.generateQuestion(req, principalA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Invalid target role 'Astronaut'"));
    }

    @Test
    @DisplayName("submitAnswer evaluates student answer and stores practice record")
    void testSubmitAnswerSuccess() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");
        InterviewQuestion q = new InterviewQuestion("iq-1", "std-1", "Explain indexing", "TECHNICAL", "MEDIUM", "SQL", "Full Stack Developer", "tips", false);
        when(questionRepository.findById("iq-1")).thenReturn(Optional.of(q));

        InterviewScoresDto scores = new InterviewScoresDto(4, 4, 4, 4, 16, 20, 80.0, "formula");
        InterviewFeedbackDto feedback = new InterviewFeedbackDto(List.of("Good explanation"), List.of("Add examples"), "Overall solid", "Practice B-trees", false);
        when(geminiInterviewService.evaluateInterviewAnswer(eq("Explain indexing"), any(), eq("TECHNICAL"), eq("MEDIUM"), eq("SQL"), eq("Full Stack Developer")))
                .thenReturn(new GeminiInterviewService.EvaluationResult(scores, feedback));
        when(practiceRepository.save(any(InterviewPractice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InterviewAnswerSubmitRequest req = new InterviewAnswerSubmitRequest("Indexes use B-trees to speed up queries.");
        InterviewPracticeResponse response = interviewService.submitAnswer("iq-1", req, principalA);

        assertTrue(response.success());
        assertNotNull(response.data());
        assertEquals("iq-1", response.data().questionId());
        assertEquals(80.0, response.data().scores().percentage());
        verify(practiceRepository, times(1)).save(any(InterviewPractice.class));
    }

    @Test
    @DisplayName("submitAnswer throws 404 when question does not exist")
    void testSubmitAnswerQuestionNotFound() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");
        when(questionRepository.findById("iq-unknown")).thenReturn(Optional.empty());

        InterviewAnswerSubmitRequest req = new InterviewAnswerSubmitRequest("Valid answer text for interview");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interviewService.submitAnswer("iq-unknown", req, principalA));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("submitAnswer enforces student isolation and throws 403 for question belonging to another student")
    void testSubmitAnswerForbiddenForOtherStudentQuestion() {
        when(careerService.resolveStudentId(principalB)).thenReturn("std-2");
        InterviewQuestion q = new InterviewQuestion("iq-1", "std-1", "Question text", "TECHNICAL", "MEDIUM", "Java", "Full Stack Developer", "tips", false);
        when(questionRepository.findById("iq-1")).thenReturn(Optional.of(q));

        InterviewAnswerSubmitRequest req = new InterviewAnswerSubmitRequest("Valid answer text for interview");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interviewService.submitAnswer("iq-1", req, principalB));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Forbidden: Access denied to interview question"));
    }

    @Test
    @DisplayName("submitAnswer throws 400 when answer is blank or shorter than 5 characters")
    void testSubmitAnswerTooShort() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");
        InterviewQuestion q = new InterviewQuestion("iq-1", "std-1", "Question text", "TECHNICAL", "MEDIUM", "Java", "Full Stack Developer", "tips", false);
        when(questionRepository.findById("iq-1")).thenReturn(Optional.of(q));

        InterviewAnswerSubmitRequest req = new InterviewAnswerSubmitRequest("abc");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> interviewService.submitAnswer("iq-1", req, principalA));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("must be at least 5 characters long"));
    }

    @Test
    @DisplayName("getHistory returns aggregated student history and weak areas")
    void testGetHistory() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");
        when(practiceRepository.findByStudentIdOrderByCreatedAtDesc("std-1")).thenReturn(Collections.emptyList());

        InterviewHistoryDataDto historyData = new InterviewHistoryDataDto(Collections.emptyList(), 0, 0.0, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList(), Collections.emptyList());
        when(geminiInterviewService.analyzeInterviewWeakAreas(Collections.emptyList())).thenReturn(historyData);

        InterviewHistoryResponse response = interviewService.getHistory(principalA);
        assertTrue(response.success());
        assertEquals(0, response.data().totalAttempts());
    }

    @Test
    @DisplayName("practiceInterviewLegacy succeeds and validates required answer")
    void testPracticeInterviewLegacy() {
        when(careerService.resolveStudentId(principalA)).thenReturn("std-1");

        LegacyInterviewPracticeRequest req = new LegacyInterviewPracticeRequest("Full Stack Developer", "What is React?", "React is a UI library.");
        LegacyInterviewPracticeResponse response = interviewService.practiceInterviewLegacy(req, principalA);

        assertTrue(response.success());
        assertEquals(85, response.data().clarityScore());
        assertEquals("Full Stack Developer", response.data().targetRole());

        // Validate error on empty answer
        LegacyInterviewPracticeRequest emptyReq = new LegacyInterviewPracticeRequest("Full Stack Developer", "Question", "");
        assertThrows(ResponseStatusException.class, () -> interviewService.practiceInterviewLegacy(emptyReq, principalA));
    }
}
