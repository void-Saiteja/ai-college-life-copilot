package com.collegecopilot.career.dto;

public class CareerAdvisorResponse {

    private boolean success;
    private CareerAdvisorDataDto data;

    public CareerAdvisorResponse() {
    }

    public CareerAdvisorResponse(boolean success, CareerAdvisorDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public CareerAdvisorDataDto getData() {
        return data;
    }

    public void setData(CareerAdvisorDataDto data) {
        this.data = data;
    }
}
