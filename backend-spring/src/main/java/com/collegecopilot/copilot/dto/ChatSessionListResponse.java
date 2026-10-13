package com.collegecopilot.copilot.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatSessionListResponse {

    private boolean success;
    private List<ChatSessionDto> data = new ArrayList<>();

    public ChatSessionListResponse() {
    }

    public ChatSessionListResponse(boolean success, List<ChatSessionDto> data) {
        this.success = success;
        this.data = data != null ? data : new ArrayList<>();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<ChatSessionDto> getData() {
        return data;
    }

    public void setData(List<ChatSessionDto> data) {
        this.data = data != null ? data : new ArrayList<>();
    }
}
