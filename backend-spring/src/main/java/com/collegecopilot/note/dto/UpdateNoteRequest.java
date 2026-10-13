package com.collegecopilot.note.dto;

public class UpdateNoteRequest {

    private String title;
    private String course;
    private String content;
    private Object tags;

    public UpdateNoteRequest() {
    }

    public UpdateNoteRequest(String title, String course, String content, Object tags) {
        this.title = title;
        this.course = course;
        this.content = content;
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Object getTags() {
        return tags;
    }

    public void setTags(Object tags) {
        this.tags = tags;
    }
}
