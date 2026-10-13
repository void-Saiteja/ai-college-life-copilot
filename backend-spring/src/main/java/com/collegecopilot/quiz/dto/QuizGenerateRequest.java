package com.collegecopilot.quiz.dto;

public class QuizGenerateRequest {

    private String subject = "CS 201";
    private String topic = "Data Structures & Algorithms";
    private String difficulty = "Medium";
    private Integer questionCount = 5;

    public QuizGenerateRequest() {
    }

    public QuizGenerateRequest(String subject, String topic, String difficulty, Integer questionCount) {
        if (subject != null && !subject.isBlank()) this.subject = subject;
        if (topic != null && !topic.isBlank()) this.topic = topic;
        if (difficulty != null && !difficulty.isBlank()) this.difficulty = difficulty;
        if (questionCount != null) this.questionCount = questionCount;
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) {
        if (subject != null && !subject.isBlank()) this.subject = subject;
    }

    public String getTopic() { return topic; }
    public void setTopic(String topic) {
        if (topic != null && !topic.isBlank()) this.topic = topic;
    }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) {
        if (difficulty != null && !difficulty.isBlank()) this.difficulty = difficulty;
    }

    public Integer getQuestionCount() { return questionCount; }
    public void setQuestionCount(Integer questionCount) {
        if (questionCount != null) this.questionCount = questionCount;
    }
}
