package com.collegecopilot.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ScheduleItemDto {

    private String id;

    @JsonProperty("student_id")
    private String studentId;

    private String course;
    private String room;
    private List<String> days;
    private String time;
    private String instructor;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    public ScheduleItemDto() {
    }

    public ScheduleItemDto(String id, String studentId, String course, String room, List<String> days, String time, String instructor, String createdAt, String updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.course = course;
        this.room = room;
        this.days = days;
        this.time = time;
        this.instructor = instructor;
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

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public List<String> getDays() {
        return days;
    }

    public void setDays(List<String> days) {
        this.days = days;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
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
