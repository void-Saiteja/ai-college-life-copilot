package com.collegecopilot.studyplanner.dto;

public class PlannerGenerateResponse {

    private boolean success = true;
    private String message = "AI Personalized Study Plan generated successfully";
    private PlannerDataDto data;

    public PlannerGenerateResponse() {
    }

    public PlannerGenerateResponse(boolean success, String message, PlannerDataDto data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public PlannerDataDto getData() { return data; }
    public void setData(PlannerDataDto data) { this.data = data; }
}
