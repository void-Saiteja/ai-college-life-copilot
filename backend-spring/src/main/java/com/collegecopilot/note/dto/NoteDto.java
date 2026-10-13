package com.collegecopilot.note.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class NoteDto {

    private String id;

    @JsonProperty("student_id")
    private String studentId;

    private String title;
    private String course;
    private String content;
    private List<String> tags = new ArrayList<>();

    private String createdAt;
    private String updatedAt;

    public NoteDto() {
    }

    public NoteDto(String id, String studentId, String title, String course, String content,
                   List<String> tags, String createdAt, String updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.title = title;
        this.course = course;
        this.content = content;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
