package com.collegecopilot.schedule.dto;

public class ScheduleDeleteResponse {

    private boolean success;
    private String message;

    public ScheduleDeleteResponse() {
    }

    public ScheduleDeleteResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
