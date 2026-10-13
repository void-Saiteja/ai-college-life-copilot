package com.collegecopilot.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExamListResponse {

    private boolean success = true;
    private ExamSummaryDto summary;
    private List<ExamDto> data;

    public ExamListResponse() {
    }

    public ExamListResponse(boolean success, ExamSummaryDto summary, List<ExamDto> data) {
        this.success = success;
        this.summary = summary;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public ExamSummaryDto getSummary() { return summary; }
    public void setSummary(ExamSummaryDto summary) { this.summary = summary; }

    public List<ExamDto> getData() { return data; }
    public void setData(List<ExamDto> data) { this.data = data; }
}
