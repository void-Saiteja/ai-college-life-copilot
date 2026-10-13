package com.collegecopilot.studyplanner.dto;

public class SessionStatusUpdateRequest {

    private String status;

    public SessionStatusUpdateRequest() {
    }

    public SessionStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
