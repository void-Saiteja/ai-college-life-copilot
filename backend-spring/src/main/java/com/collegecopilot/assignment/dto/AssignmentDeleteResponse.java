package com.collegecopilot.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentDeleteResponse {

    private boolean success = true;
    private String message = "Assignment deleted successfully";

    public AssignmentDeleteResponse() {
    }

    public AssignmentDeleteResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
