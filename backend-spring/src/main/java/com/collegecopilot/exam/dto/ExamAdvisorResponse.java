package com.collegecopilot.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExamAdvisorResponse {

    private boolean success = true;
    private ExamAdvisorDataDto data;

    public ExamAdvisorResponse() {
    }

    public ExamAdvisorResponse(boolean success, ExamAdvisorDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public ExamAdvisorDataDto getData() { return data; }
    public void setData(ExamAdvisorDataDto data) { this.data = data; }
}
