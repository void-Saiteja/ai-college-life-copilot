package com.collegecopilot.student.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizHistorySummaryDto {

    private int totalAttempts;
    private List<String> weakTopics;
    private Object recentAttempt;

    public QuizHistorySummaryDto() {
    }

    public QuizHistorySummaryDto(int totalAttempts, List<String> weakTopics, Object recentAttempt) {
        this.totalAttempts = totalAttempts;
        this.weakTopics = weakTopics;
        this.recentAttempt = recentAttempt;
    }

    public int getTotalAttempts() { return totalAttempts; }
    public void setTotalAttempts(int totalAttempts) { this.totalAttempts = totalAttempts; }

    public List<String> getWeakTopics() { return weakTopics; }
    public void setWeakTopics(List<String> weakTopics) { this.weakTopics = weakTopics; }

    public Object getRecentAttempt() { return recentAttempt; }
    public void setRecentAttempt(Object recentAttempt) { this.recentAttempt = recentAttempt; }
}
