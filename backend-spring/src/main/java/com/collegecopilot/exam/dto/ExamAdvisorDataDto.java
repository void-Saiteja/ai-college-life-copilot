package com.collegecopilot.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExamAdvisorDataDto {

    private String summary;
    private String recommendation;
    private List<ExamDto> prioritizedExams;

    @JsonProperty("isFallback")
    private boolean isFallback;

    public ExamAdvisorDataDto() {
    }

    public ExamAdvisorDataDto(String summary, String recommendation, List<ExamDto> prioritizedExams, boolean isFallback) {
        this.summary = summary;
        this.recommendation = recommendation;
        this.prioritizedExams = prioritizedExams;
        this.isFallback = isFallback;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public List<ExamDto> getPrioritizedExams() { return prioritizedExams; }
    public void setPrioritizedExams(List<ExamDto> prioritizedExams) { this.prioritizedExams = prioritizedExams; }

    @JsonProperty("isFallback")
    public boolean isFallback() { return isFallback; }

    @JsonProperty("isFallback")
    public void setFallback(boolean fallback) { isFallback = fallback; }
}
