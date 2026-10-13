package com.collegecopilot.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class QuizAdvisorDataDto {

    private String summary;
    private String recommendation;
    private List<String> weakTopics = new ArrayList<>();
    private List<String> strongTopics = new ArrayList<>();

    @JsonProperty("isFallback")
    private boolean isFallback;

    public QuizAdvisorDataDto() {
    }

    public QuizAdvisorDataDto(String summary, String recommendation, List<String> weakTopics, List<String> strongTopics, boolean isFallback) {
        this.summary = summary;
        this.recommendation = recommendation;
        this.weakTopics = weakTopics != null ? weakTopics : new ArrayList<>();
        this.strongTopics = strongTopics != null ? strongTopics : new ArrayList<>();
        this.isFallback = isFallback;
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public List<String> getWeakTopics() { return weakTopics; }
    public void setWeakTopics(List<String> weakTopics) { this.weakTopics = weakTopics != null ? weakTopics : new ArrayList<>(); }

    public List<String> getStrongTopics() { return strongTopics; }
    public void setStrongTopics(List<String> strongTopics) { this.strongTopics = strongTopics != null ? strongTopics : new ArrayList<>(); }

    public boolean isFallback() { return isFallback; }
    public void setFallback(boolean fallback) { isFallback = fallback; }
}
