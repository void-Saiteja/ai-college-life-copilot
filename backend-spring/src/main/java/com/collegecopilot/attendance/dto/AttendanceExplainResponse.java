package com.collegecopilot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceExplainResponse {

    private boolean success = true;
    private ExplainDataDto data;

    public AttendanceExplainResponse() {
    }

    public AttendanceExplainResponse(boolean success, ExplainDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ExplainDataDto getData() {
        return data;
    }

    public void setData(ExplainDataDto data) {
        this.data = data;
    }
}
