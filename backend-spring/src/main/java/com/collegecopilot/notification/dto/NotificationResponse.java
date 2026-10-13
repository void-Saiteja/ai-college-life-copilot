package com.collegecopilot.notification.dto;

public class NotificationResponse {

    private boolean success;
    private String message;
    private NotificationItemDto data;

    public NotificationResponse() {
    }

    public NotificationResponse(boolean success, String message, NotificationItemDto data) {
        this.success = success;
        this.message = message;
        this.data = data;
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

    public NotificationItemDto getData() {
        return data;
    }

    public void setData(NotificationItemDto data) {
        this.data = data;
    }
}
