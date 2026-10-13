package com.collegecopilot.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizSummaryDto {

    private String id;
    private String subject;
    private String topic;
    private String difficulty;
    private Integer questionCount;
    private String status;
    private Integer score;
    private Double percentage;

    @JsonProperty("created_at")
    private String createdAt;

    public QuizSummaryDto() {
    }

    public QuizSummaryDto(String id, String subject, String topic, String difficulty, Integer questionCount, String status, Integer score, Double percentage, String createdAt) {
        this.id = id;
        this.subject = subject;
        this.topic = topic;
        this.difficulty = difficulty;
        this.questionCount = questionCount;
        this.status = status;
        this.score = score;
        this.percentage = percentage;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
