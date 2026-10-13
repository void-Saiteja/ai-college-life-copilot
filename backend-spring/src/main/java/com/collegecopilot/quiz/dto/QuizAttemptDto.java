package com.collegecopilot.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizAttemptDto {

    private String id;

    @JsonProperty("quiz_id")
    private String quizId;

    @JsonProperty("student_id")
    private String studentId;

    private String subject;
    private String topic;
    private Integer score;

    @JsonProperty("total_questions")
    private Integer totalQuestions;

    private BigDecimal percentage;

    @JsonProperty("created_at")
    private String createdAt;

    public QuizAttemptDto() {
    }

    public QuizAttemptDto(String id, String quizId, String studentId, String subject, String topic, Integer score, Integer totalQuestions, BigDecimal percentage, String createdAt) {
        this.id = id;
        this.quizId = quizId;
        this.studentId = studentId;
        this.subject = subject;
        this.topic = topic;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.percentage = percentage;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }

    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
