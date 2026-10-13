package com.collegecopilot.quiz.dto;

import java.util.ArrayList;
import java.util.List;

public class QuizHistoryDataDto {

    private List<QuizAttemptDto> attempts = new ArrayList<>();
    private List<QuizSummaryDto> quizzes = new ArrayList<>();

    public QuizHistoryDataDto() {
    }

    public QuizHistoryDataDto(List<QuizAttemptDto> attempts, List<QuizSummaryDto> quizzes) {
        this.attempts = attempts != null ? attempts : new ArrayList<>();
        this.quizzes = quizzes != null ? quizzes : new ArrayList<>();
    }

    public List<QuizAttemptDto> getAttempts() { return attempts; }
    public void setAttempts(List<QuizAttemptDto> attempts) { this.attempts = attempts != null ? attempts : new ArrayList<>(); }

    public List<QuizSummaryDto> getQuizzes() { return quizzes; }
    public void setQuizzes(List<QuizSummaryDto> quizzes) { this.quizzes = quizzes != null ? quizzes : new ArrayList<>(); }
}
