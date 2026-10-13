package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CareerAdvisorDataDto {

    private String summary;
    private String recommendation;
    private SkillGapAnalysisDto skillGapAnalysis;

    @JsonProperty("isFallback")
    private boolean isFallback;

    public CareerAdvisorDataDto() {
    }

    public CareerAdvisorDataDto(String summary, String recommendation, SkillGapAnalysisDto skillGapAnalysis, boolean isFallback) {
        this.summary = summary;
        this.recommendation = recommendation;
        this.skillGapAnalysis = skillGapAnalysis;
        this.isFallback = isFallback;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public SkillGapAnalysisDto getSkillGapAnalysis() {
        return skillGapAnalysis;
    }

    public void setSkillGapAnalysis(SkillGapAnalysisDto skillGapAnalysis) {
        this.skillGapAnalysis = skillGapAnalysis;
    }

    public boolean isFallback() {
        return isFallback;
    }

    public void setFallback(boolean fallback) {
        isFallback = fallback;
    }
}
