package com.collegecopilot.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationGenerateRequest {

    @JsonProperty("student_id")
    private String studentId;

    public NotificationGenerateRequest() {
    }

    public NotificationGenerateRequest(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}
