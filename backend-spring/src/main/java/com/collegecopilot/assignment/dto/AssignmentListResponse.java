package com.collegecopilot.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentListResponse {

    private boolean success = true;
    private AssignmentSummaryDto summary;
    private List<AssignmentDto> data;

    public AssignmentListResponse() {
    }

    public AssignmentListResponse(boolean success, AssignmentSummaryDto summary, List<AssignmentDto> data) {
        this.success = success;
        this.summary = summary;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public AssignmentSummaryDto getSummary() {
        return summary;
    }

    public void setSummary(AssignmentSummaryDto summary) {
        this.summary = summary;
    }

    public List<AssignmentDto> getData() {
        return data;
    }

    public void setData(List<AssignmentDto> data) {
        this.data = data;
    }
}
