package com.collegecopilot.quiz.dto;

public class QuizHistoryResponse {

    private boolean success = true;
    private QuizHistoryDataDto data;

    public QuizHistoryResponse() {
    }

    public QuizHistoryResponse(boolean success, QuizHistoryDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public QuizHistoryDataDto getData() { return data; }
    public void setData(QuizHistoryDataDto data) { this.data = data; }
}
