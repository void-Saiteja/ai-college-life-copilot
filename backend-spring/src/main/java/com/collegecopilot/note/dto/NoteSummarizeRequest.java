package com.collegecopilot.note.dto;

public class NoteSummarizeRequest {

    private String title;
    private String course;
    private String content;
    private String noteId;

    public NoteSummarizeRequest() {
    }

    public NoteSummarizeRequest(String title, String course, String content, String noteId) {
        this.title = title;
        this.course = course;
        this.content = content;
        this.noteId = noteId;
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

    public String getNoteId() {
        return noteId;
    }

    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }
}
