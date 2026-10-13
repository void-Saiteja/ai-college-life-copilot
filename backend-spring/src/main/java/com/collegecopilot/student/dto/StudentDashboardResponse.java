package com.collegecopilot.student.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudentDashboardResponse {

    private boolean success = true;
    private DashboardDataDto data;

    public StudentDashboardResponse() {
    }

    public StudentDashboardResponse(boolean success, DashboardDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public DashboardDataDto getData() {
        return data;
    }

    public void setData(DashboardDataDto data) {
        this.data = data;
    }
}
