package com.collegecopilot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExplainDataDto {

    private AttendanceMetrics metrics;
    private String explanation;

    @JsonProperty("isFallback")
    private boolean isFallback;

    public ExplainDataDto() {
    }

    public ExplainDataDto(AttendanceMetrics metrics, String explanation, boolean isFallback) {
        this.metrics = metrics;
        this.explanation = explanation;
        this.isFallback = isFallback;
    }

    public AttendanceMetrics getMetrics() {
        return metrics;
    }

    public void setMetrics(AttendanceMetrics metrics) {
        this.metrics = metrics;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    @JsonProperty("isFallback")
    public boolean isFallback() {
        return isFallback;
    }

    @JsonProperty("isFallback")
    public void setFallback(boolean fallback) {
        isFallback = fallback;
    }
}
