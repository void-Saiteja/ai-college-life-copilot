package com.collegecopilot.quiz;

import com.collegecopilot.quiz.dto.QuizQuestionDto;

import java.util.ArrayList;
import java.util.List;

public class QuizRecord {

    private String id;
    private String studentId;
    private String subject;
    private String topic;
    private String difficulty;
    private int questionCount;
    private List<QuizQuestionDto> questions = new ArrayList<>();
    private String status = "Pending";
    private Integer score;
    private Double percentage;
    private String createdAt;
    private String completedAt;

    public QuizRecord() {
    }

    public QuizRecord(String id, String studentId, String subject, String topic, String difficulty, int questionCount, List<QuizQuestionDto> questions, String createdAt) {
        this.id = id;
        this.studentId = studentId;
        this.subject = subject;
        this.topic = topic;
        this.difficulty = difficulty;
        this.questionCount = questionCount;
        this.questions = questions != null ? questions : new ArrayList<>();
        this.status = "Pending";
        this.createdAt = createdAt;
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

    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

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
