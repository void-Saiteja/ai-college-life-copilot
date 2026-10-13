package com.collegecopilot.notification.dto;

public class NotificationListResponse {

    private boolean success;
    private NotificationListData data;

    public NotificationListResponse() {
    }

    public NotificationListResponse(boolean success, NotificationListData data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public NotificationListData getData() {
        return data;
    }

    public void setData(NotificationListData data) {
        this.data = data;
    }
}
