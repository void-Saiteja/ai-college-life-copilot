package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CareerProfileResponse {

    private boolean success;
    private CareerProfileDataDto data;
    private String message;

    public CareerProfileResponse() {
    }

    public CareerProfileResponse(boolean success, CareerProfileDataDto data) {
        this.success = success;
        this.data = data;
    }

    public CareerProfileResponse(boolean success, CareerProfileDataDto data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public CareerProfileDataDto getData() {
        return data;
    }

    public void setData(CareerProfileDataDto data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
