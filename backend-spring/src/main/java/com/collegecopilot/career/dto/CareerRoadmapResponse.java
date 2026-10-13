package com.collegecopilot.career.dto;

public class CareerRoadmapResponse {

    private boolean success;
    private CareerRoadmapDataDto data;

    public CareerRoadmapResponse() {
    }

    public CareerRoadmapResponse(boolean success, CareerRoadmapDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public CareerRoadmapDataDto getData() {
        return data;
    }

    public void setData(CareerRoadmapDataDto data) {
        this.data = data;
    }
}
