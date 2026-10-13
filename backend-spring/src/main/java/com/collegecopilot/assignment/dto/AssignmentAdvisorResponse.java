package com.collegecopilot.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentAdvisorResponse {

    private boolean success = true;
    private AdvisorDataDto data;

    public AssignmentAdvisorResponse() {
    }

    public AssignmentAdvisorResponse(boolean success, AdvisorDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public AdvisorDataDto getData() {
        return data;
    }

    public void setData(AdvisorDataDto data) {
        this.data = data;
    }
}
