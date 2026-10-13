package com.collegecopilot.copilot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageDto {

    private String id;

    @JsonProperty("session_id")
    private String sessionId;

    private String sender;
    private String content;
    private List<SourceMetadataDto> sources;

    @JsonProperty("created_at")
    private String createdAt;

    public ChatMessageDto() {
    }

    public ChatMessageDto(String id, String sessionId, String sender, String content, List<SourceMetadataDto> sources, String createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.sender = sender;
        this.content = content;
        this.sources = sources;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<SourceMetadataDto> getSources() {
        return sources;
    }

    public void setSources(List<SourceMetadataDto> sources) {
        this.sources = sources;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
