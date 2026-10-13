package com.collegecopilot.quiz.dto;

public class QuizDetailResponse {

    private boolean success = true;
    private QuizRecordDto data;

    public QuizDetailResponse() {
    }

    public QuizDetailResponse(boolean success, QuizRecordDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public QuizRecordDto getData() { return data; }
    public void setData(QuizRecordDto data) { this.data = data; }
}
