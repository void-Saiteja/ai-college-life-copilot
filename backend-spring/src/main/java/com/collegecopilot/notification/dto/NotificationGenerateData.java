package com.collegecopilot.notification.dto;

public class NotificationGenerateData {

    private int createdCount;
    private int totalCount;
    private int unreadCount;

    public NotificationGenerateData() {
    }

    public NotificationGenerateData(int createdCount, int totalCount, int unreadCount) {
        this.createdCount = createdCount;
        this.totalCount = totalCount;
        this.unreadCount = unreadCount;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
