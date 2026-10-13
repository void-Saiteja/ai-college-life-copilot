package com.collegecopilot.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationGenerateResponse {

    private boolean success;
    private String message;
    private NotificationGenerateData data;
    private Integer createdCount;

    public NotificationGenerateResponse() {
    }

    public NotificationGenerateResponse(boolean success, NotificationGenerateData data) {
        this.success = success;
        this.data = data;
    }

    public NotificationGenerateResponse(boolean success, String message, Integer createdCount) {
        this.success = success;
        this.message = message;
        this.createdCount = createdCount;
    }

    public NotificationGenerateResponse(boolean success, String message, NotificationGenerateData data, Integer createdCount) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.createdCount = createdCount;
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

    public NotificationGenerateData getData() {
        return data;
    }

    public void setData(NotificationGenerateData data) {
        this.data = data;
    }

    public Integer getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(Integer createdCount) {
        this.createdCount = createdCount;
    }
}
