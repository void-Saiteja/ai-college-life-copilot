package com.collegecopilot.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdvisorDataDto {

    private String summary;
    private String recommendation;
    private List<AssignmentDto> prioritizedTasks;

    @JsonProperty("isFallback")
    private boolean isFallback;

    public AdvisorDataDto() {
    }

    public AdvisorDataDto(String summary, String recommendation, List<AssignmentDto> prioritizedTasks, boolean isFallback) {
        this.summary = summary;
        this.recommendation = recommendation;
        this.prioritizedTasks = prioritizedTasks;
        this.isFallback = isFallback;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public List<AssignmentDto> getPrioritizedTasks() { return prioritizedTasks; }
    public void setPrioritizedTasks(List<AssignmentDto> prioritizedTasks) { this.prioritizedTasks = prioritizedTasks; }

    @JsonProperty("isFallback")
    public boolean isFallback() { return isFallback; }

    @JsonProperty("isFallback")
    public void setFallback(boolean fallback) { isFallback = fallback; }
}
