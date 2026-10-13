package com.collegecopilot.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExamSingleResponse {

    private boolean success = true;
    private ExamDto data;

    public ExamSingleResponse() {
    }

    public ExamSingleResponse(boolean success, ExamDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public ExamDto getData() { return data; }
    public void setData(ExamDto data) { this.data = data; }
}
