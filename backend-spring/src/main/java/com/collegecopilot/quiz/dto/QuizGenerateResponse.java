package com.collegecopilot.quiz.dto;

public class QuizGenerateResponse {

    private boolean success = true;
    private QuizGeneratedDataDto data;

    public QuizGenerateResponse() {
    }

    public QuizGenerateResponse(boolean success, QuizGeneratedDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public QuizGeneratedDataDto getData() { return data; }
    public void setData(QuizGeneratedDataDto data) { this.data = data; }
}
