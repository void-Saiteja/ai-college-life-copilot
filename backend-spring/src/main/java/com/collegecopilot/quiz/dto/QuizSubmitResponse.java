package com.collegecopilot.quiz.dto;

public class QuizSubmitResponse {

    private boolean success = true;
    private QuizSubmissionResultDto data;

    public QuizSubmitResponse() {
    }

    public QuizSubmitResponse(boolean success, QuizSubmissionResultDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public QuizSubmissionResultDto getData() { return data; }
    public void setData(QuizSubmissionResultDto data) { this.data = data; }
}
