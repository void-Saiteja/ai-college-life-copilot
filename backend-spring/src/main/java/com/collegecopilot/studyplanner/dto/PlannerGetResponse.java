package com.collegecopilot.studyplanner.dto;

public class PlannerGetResponse {

    private boolean success = true;
    private PlannerDataDto data;

    public PlannerGetResponse() {
    }

    public PlannerGetResponse(boolean success, PlannerDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public PlannerDataDto getData() { return data; }
    public void setData(PlannerDataDto data) { this.data = data; }
}
