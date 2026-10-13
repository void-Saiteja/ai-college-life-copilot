package com.collegecopilot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceProjectionResponse {

    private boolean success = true;
    private ProjectionDataDto data;

    public AttendanceProjectionResponse() {
    }

    public AttendanceProjectionResponse(boolean success, ProjectionDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ProjectionDataDto getData() {
        return data;
    }

    public void setData(ProjectionDataDto data) {
        this.data = data;
    }
}
