package com.collegecopilot.auth.dto;

public class ErrorResponse {

    private boolean success = false;
    private String error;

    public ErrorResponse() {
    }

    public ErrorResponse(String error) {
        this.success = false;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
