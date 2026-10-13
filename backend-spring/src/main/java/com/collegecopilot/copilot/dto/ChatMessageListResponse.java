package com.collegecopilot.copilot.dto;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageListResponse {

    private boolean success;
    private List<ChatMessageDto> data = new ArrayList<>();

    public ChatMessageListResponse() {
    }

    public ChatMessageListResponse(boolean success, List<ChatMessageDto> data) {
        this.success = success;
        this.data = data != null ? data : new ArrayList<>();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<ChatMessageDto> getData() {
        return data;
    }

    public void setData(List<ChatMessageDto> data) {
        this.data = data != null ? data : new ArrayList<>();
    }
}
