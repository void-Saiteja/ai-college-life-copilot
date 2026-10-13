package com.collegecopilot.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class QuizGeneratedDataDto {

    private String quizId;
    private String subject;
    private String topic;
    private String difficulty;
    private int totalQuestions;
    private List<QuizQuestionDto> questions = new ArrayList<>();

    @JsonProperty("isFallback")
    private boolean isFallback;

    public QuizGeneratedDataDto() {
    }

    public QuizGeneratedDataDto(String quizId, String subject, String topic, String difficulty, int totalQuestions, List<QuizQuestionDto> questions, boolean isFallback) {
        this.quizId = quizId;
        this.subject = subject;
        this.topic = topic;
        this.difficulty = difficulty;
        this.totalQuestions = totalQuestions;
        this.questions = questions != null ? questions : new ArrayList<>();
        this.isFallback = isFallback;
    }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public List<QuizQuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestionDto> questions) { this.questions = questions; }

    public boolean isFallback() { return isFallback; }
    public void setFallback(boolean fallback) { isFallback = fallback; }
}
