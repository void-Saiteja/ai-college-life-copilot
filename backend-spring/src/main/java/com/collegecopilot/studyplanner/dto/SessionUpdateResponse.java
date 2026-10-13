package com.collegecopilot.studyplanner.dto;

public class SessionUpdateResponse {

    private boolean success = true;
    private StudySessionDto data;

    public SessionUpdateResponse() {
    }

    public SessionUpdateResponse(boolean success, StudySessionDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public StudySessionDto getData() { return data; }
    public void setData(StudySessionDto data) { this.data = data; }
}
