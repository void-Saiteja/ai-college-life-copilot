package com.collegecopilot.notification.dto;

import java.util.ArrayList;
import java.util.List;

public class NotificationListData {

    private List<NotificationItemDto> notifications = new ArrayList<>();
    private int unreadCount;
    private int count;

    public NotificationListData() {
    }

    public NotificationListData(List<NotificationItemDto> notifications, int unreadCount, int count) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
        this.unreadCount = unreadCount;
        this.count = count;
    }

    public List<NotificationItemDto> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<NotificationItemDto> notifications) {
        this.notifications = notifications != null ? notifications : new ArrayList<>();
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
