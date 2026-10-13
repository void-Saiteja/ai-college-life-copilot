package com.collegecopilot.notification.dto;

public class NotificationReadAllResponse {

    private boolean success;
    private String message;
    private int updatedCount;

    public NotificationReadAllResponse() {
    }

    public NotificationReadAllResponse(boolean success, String message, int updatedCount) {
        this.success = success;
        this.message = message;
        this.updatedCount = updatedCount;
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

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }
}
