package com.collegecopilot.studyplanner.dto;

import java.util.ArrayList;
import java.util.List;

public class PlannerReplanResponse {

    private boolean success = true;
    private String message = "Replanned remaining sessions successfully! Completed sessions preserved.";
    private List<StudySessionDto> data = new ArrayList<>();

    public PlannerReplanResponse() {
    }

    public PlannerReplanResponse(boolean success, String message, List<StudySessionDto> data) {
        this.success = success;
        this.message = message;
        this.data = data != null ? data : new ArrayList<>();
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<StudySessionDto> getData() { return data; }
    public void setData(List<StudySessionDto> data) { this.data = data; }
}
