package com.collegecopilot.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnnouncementDto {

    private String id;
    private String title;
    private String content;

    @JsonProperty("posted_by")
    private String postedBy;

    @JsonProperty("created_at")
    private String createdAt;

    public AnnouncementDto() {
    }

    public AnnouncementDto(String id, String title, String content, String postedBy, String createdAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.postedBy = postedBy;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
