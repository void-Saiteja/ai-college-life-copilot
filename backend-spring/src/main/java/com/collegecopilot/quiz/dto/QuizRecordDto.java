package com.collegecopilot.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizRecordDto {

    private String id;

    @JsonProperty("student_id")
    private String studentId;

    private String subject;
    private String topic;
    private String difficulty;

    @JsonProperty("question_count")
    private Integer questionCount;

    private List<QuizQuestionDto> questions = new ArrayList<>();
    private String status;
    private Integer score;
    private Double percentage;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("completed_at")
    private String completedAt;

    public QuizRecordDto() {
    }

    public QuizRecordDto(String id, String studentId, String subject, String topic, String difficulty, Integer questionCount, List<QuizQuestionDto> questions, String status, Integer score, Double percentage, String createdAt, String completedAt) {
        this.id = id;
        this.studentId = studentId;
        this.subject = subject;
        this.topic = topic;
        this.difficulty = difficulty;
        this.questionCount = questionCount;
        this.questions = questions != null ? questions : new ArrayList<>();
        this.status = status;
        this.score = score;
        this.percentage = percentage;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) { this.questionCount = questionCount; }

    public List<QuizQuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<QuizQuestionDto> questions) { this.questions = questions != null ? questions : new ArrayList<>(); }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
}
