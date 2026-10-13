package com.collegecopilot.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateAnnouncementRequest {

    @NotBlank(message = "Title and content are required")
    private String title;

    @NotBlank(message = "Title and content are required")
    private String content;

    public CreateAnnouncementRequest() {
    }

    public CreateAnnouncementRequest(String title, String content) {
        this.title = title;
        this.content = content;
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
}
