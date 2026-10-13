package com.collegecopilot.quiz.dto;

public class QuizAdvisorResponse {

    private boolean success = true;
    private QuizAdvisorDataDto data;

    public QuizAdvisorResponse() {
    }

    public QuizAdvisorResponse(boolean success, QuizAdvisorDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public QuizAdvisorDataDto getData() { return data; }
    public void setData(QuizAdvisorDataDto data) { this.data = data; }
}
