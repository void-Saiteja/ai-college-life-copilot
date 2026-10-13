package com.collegecopilot.quiz.dto;

public class QuizDeleteResponse {

    private boolean success = true;
    private String message = "Quiz deleted successfully";

    public QuizDeleteResponse() {
    }

    public QuizDeleteResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
