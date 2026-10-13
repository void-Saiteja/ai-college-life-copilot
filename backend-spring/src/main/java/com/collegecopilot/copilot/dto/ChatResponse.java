package com.collegecopilot.copilot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {

    private boolean success;
    private ChatResponseData data;

    public ChatResponse() {
    }

    public ChatResponse(boolean success, ChatResponseData data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ChatResponseData getData() {
        return data;
    }

    public void setData(ChatResponseData data) {
        this.data = data;
    }

    public static class ChatResponseData {
        private String sessionId;
        private ChatMessageDto userMessage;
        private ChatMessageDto assistantMessage;

        public ChatResponseData() {
        }

        public ChatResponseData(String sessionId, ChatMessageDto userMessage, ChatMessageDto assistantMessage) {
            this.sessionId = sessionId;
            this.userMessage = userMessage;
            this.assistantMessage = assistantMessage;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public ChatMessageDto getUserMessage() {
            return userMessage;
        }

        public void setUserMessage(ChatMessageDto userMessage) {
            this.userMessage = userMessage;
        }

        public ChatMessageDto getAssistantMessage() {
            return assistantMessage;
        }

        public void setAssistantMessage(ChatMessageDto assistantMessage) {
            this.assistantMessage = assistantMessage;
        }
    }
}
