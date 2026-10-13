package com.collegecopilot.copilot.dto;

public class QuickChatResponse {

    private boolean success;
    private String reply;
    private String timestamp;

    public QuickChatResponse() {
    }

    public QuickChatResponse(boolean success, String reply, String timestamp) {
        this.success = success;
        this.reply = reply;
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
