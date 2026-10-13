package com.collegecopilot.studyplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StructuredPlanDto {

    private String summary;
    private List<PlanPriorityDto> priorities = new ArrayList<>();

    @JsonProperty("daily_plan")
    private List<DailyPlanDto> dailyPlan = new ArrayList<>();

    private List<String> recommendations = new ArrayList<>();

    public StructuredPlanDto() {
    }

    public StructuredPlanDto(String summary, List<PlanPriorityDto> priorities, List<DailyPlanDto> dailyPlan, List<String> recommendations) {
        this.summary = summary;
        this.priorities = priorities != null ? priorities : new ArrayList<>();
        this.dailyPlan = dailyPlan != null ? dailyPlan : new ArrayList<>();
        this.recommendations = recommendations != null ? recommendations : new ArrayList<>();
    }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<PlanPriorityDto> getPriorities() { return priorities; }
    public void setPriorities(List<PlanPriorityDto> priorities) { this.priorities = priorities; }

    public List<DailyPlanDto> getDailyPlan() { return dailyPlan; }
    public void setDailyPlan(List<DailyPlanDto> dailyPlan) { this.dailyPlan = dailyPlan; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
}
