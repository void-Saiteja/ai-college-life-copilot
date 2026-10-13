package com.collegecopilot.notification.dto;

public class UnreadCountResponse {

    private boolean success;
    private int count;

    public UnreadCountResponse() {
    }

    public UnreadCountResponse(boolean success, int count) {
        this.success = success;
        this.count = count;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
