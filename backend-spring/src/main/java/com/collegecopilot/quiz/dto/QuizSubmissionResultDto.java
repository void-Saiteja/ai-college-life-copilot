package com.collegecopilot.quiz.dto;

import java.util.ArrayList;
import java.util.List;

public class QuizSubmissionResultDto {

    private String attemptId;
    private String quizId;
    private String subject;
    private String topic;
    private int score;
    private int totalQuestions;
    private int correctCount;
    private int incorrectCount;
    private int unansweredCount;
    private double percentage;
    private String masteryLevel;
    private List<QuizQuestionResultDto> results = new ArrayList<>();
    private List<String> weakTopics = new ArrayList<>();
    private List<String> recommendedTopics = new ArrayList<>();
    private String aiFeedback;

    public QuizSubmissionResultDto() {
    }

    public QuizSubmissionResultDto(String attemptId, String quizId, String subject, String topic, int score, int totalQuestions, int correctCount, int incorrectCount, int unansweredCount, double percentage, String masteryLevel, List<QuizQuestionResultDto> results, List<String> weakTopics, List<String> recommendedTopics, String aiFeedback) {
        this.attemptId = attemptId;
        this.quizId = quizId;
        this.subject = subject;
        this.topic = topic;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.correctCount = correctCount;
        this.incorrectCount = incorrectCount;
        this.unansweredCount = unansweredCount;
        this.percentage = percentage;
        this.masteryLevel = masteryLevel;
        this.results = results != null ? results : new ArrayList<>();
        this.weakTopics = weakTopics != null ? weakTopics : new ArrayList<>();
        this.recommendedTopics = recommendedTopics != null ? recommendedTopics : new ArrayList<>();
        this.aiFeedback = aiFeedback;
    }

    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }

    public int getIncorrectCount() { return incorrectCount; }
    public void setIncorrectCount(int incorrectCount) { this.incorrectCount = incorrectCount; }

    public int getUnansweredCount() { return unansweredCount; }
    public void setUnansweredCount(int unansweredCount) { this.unansweredCount = unansweredCount; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    public String getMasteryLevel() { return masteryLevel; }
    public void setMasteryLevel(String masteryLevel) { this.masteryLevel = masteryLevel; }

    public List<QuizQuestionResultDto> getResults() { return results; }
    public void setResults(List<QuizQuestionResultDto> results) { this.results = results; }

    public List<String> getWeakTopics() { return weakTopics; }
    public void setWeakTopics(List<String> weakTopics) { this.weakTopics = weakTopics; }

    public List<String> getRecommendedTopics() { return recommendedTopics; }
    public void setRecommendedTopics(List<String> recommendedTopics) { this.recommendedTopics = recommendedTopics; }

    public String getAiFeedback() { return aiFeedback; }
    public void setAiFeedback(String aiFeedback) { this.aiFeedback = aiFeedback; }
}
