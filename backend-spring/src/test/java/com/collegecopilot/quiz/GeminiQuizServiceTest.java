package com.collegecopilot.quiz;

import com.collegecopilot.quiz.dto.QuizAdvisorDataDto;
import com.collegecopilot.quiz.dto.QuizQuestionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeminiQuizServiceTest {

    @Test
    @DisplayName("validateQuizQuestion verifies required fields, options count, and valid correctAnswer index")
    void validateQuizQuestion_behavior() {
        GeminiQuizService service = new GeminiQuizService("");

        // Valid question
        QuizQuestionDto valid = new QuizQuestionDto(1, "What is Big-O?", List.of("A", "B", "C"), 0, "Explanation");
        assertTrue(service.validateQuizQuestion(valid));

        // Missing question text
        QuizQuestionDto emptyQuestion = new QuizQuestionDto(1, "", List.of("A", "B"), 0, "Explanation");
        assertFalse(service.validateQuizQuestion(emptyQuestion));

        // Too few options (< 2)
        QuizQuestionDto oneOption = new QuizQuestionDto(1, "Question?", List.of("A"), 0, "Explanation");
        assertFalse(service.validateQuizQuestion(oneOption));

        // Out of bounds correctAnswer index
        QuizQuestionDto outOfBounds = new QuizQuestionDto(1, "Question?", List.of("A", "B"), 2, "Explanation");
        assertFalse(service.validateQuizQuestion(outOfBounds));

        // Negative correctAnswer
        QuizQuestionDto negativeAnswer = new QuizQuestionDto(1, "Question?", List.of("A", "B"), -1, "Explanation");
        assertFalse(service.validateQuizQuestion(negativeAnswer));

        // Null question
        assertFalse(service.validateQuizQuestion(null));
    }

    @Test
    @DisplayName("generateQuizQuestions uses fallback when API key is missing or mock")
    void generateQuizQuestions_whenNoApiKey_usesFallback() {
        GeminiQuizService service = new GeminiQuizService("");

        GeminiQuizService.QuizGenerationResult result = service.generateQuizQuestions(
                "CS 201", "Binary Trees", "Medium", 3, List.of("- Midterm Exam in 5 days")
        );

        assertNotNull(result);
        assertTrue(result.isFallback());
        assertNotNull(result.questions());
        assertEquals(3, result.questions().size());

        for (QuizQuestionDto q : result.questions()) {
            assertTrue(service.validateQuizQuestion(q));
            assertNotNull(q.getExplanation());
        }
    }

    @Test
    @DisplayName("generateAdvisorAdvice calculates deterministic weak and strong topics when API key is unset")
    void generateAdvisorAdvice_whenNoApiKey_usesFallbackCalculations() {
        GeminiQuizService service = new GeminiQuizService("");

        List<GeminiQuizService.AdvisorAttempt> attempts = List.of(
                new GeminiQuizService.AdvisorAttempt("Data Structures", "Trees", 50.0, 2, 4), // weak < 60
                new GeminiQuizService.AdvisorAttempt("Database Systems", "SQL Joins", 90.0, 9, 10) // strong >= 80
        );

        QuizAdvisorDataDto advice = service.generateAdvisorAdvice(attempts, List.of("- DBMS Final Exam in 2 days"));

        assertNotNull(advice);
        assertTrue(advice.isFallback());
        assertNotNull(advice.getRecommendation());
        assertNotNull(advice.getWeakTopics());
        assertNotNull(advice.getStrongTopics());

        // Weak topic identified
        assertTrue(advice.getWeakTopics().stream().anyMatch(t -> t.contains("Trees")));
        // Strong topic identified
        assertTrue(advice.getStrongTopics().stream().anyMatch(t -> t.contains("SQL Joins")));
    }

    @Test
    @DisplayName("generateAdvisorAdvice returns baseline advice when no attempts exist")
    void generateAdvisorAdvice_emptyAttempts_returnsBaselineAdvice() {
        GeminiQuizService service = new GeminiQuizService("");

        QuizAdvisorDataDto advice = service.generateAdvisorAdvice(List.of(), List.of());

        assertNotNull(advice);
        assertEquals("No quiz performance history found.", advice.getSummary());
        assertTrue(advice.getRecommendation().contains("You have not taken any quizzes yet"));
        assertTrue(advice.getWeakTopics().isEmpty());
        assertTrue(advice.getStrongTopics().isEmpty());
    }
}
