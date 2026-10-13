package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public record InterviewHistoryDataDto(
        @JsonProperty("history") List<InterviewPracticeDto> history,
        @JsonProperty("total_attempts") int totalAttempts,
        @JsonProperty("average_score") double averageScore,
        @JsonProperty("category_breakdown") Map<String, CategoryBreakdownDto> categoryBreakdown,
        @JsonProperty("skill_breakdown") Map<String, SkillBreakdownDto> skillBreakdown,
        @JsonProperty("weak_areas") List<WeakAreaDto> weakAreas,
        @JsonProperty("strong_areas") List<StrongAreaDto> strongAreas
) {
    public record CategoryBreakdownDto(
            @JsonProperty("attempts") int attempts,
            @JsonProperty("average_percentage") double averagePercentage
    ) {}

    public record SkillBreakdownDto(
            @JsonProperty("attempts") int attempts,
            @JsonProperty("average_percentage") double averagePercentage
    ) {}

    public record WeakAreaDto(
            @JsonProperty("category") String category,
            @JsonProperty("average_percentage") double averagePercentage,
            @JsonProperty("status") String status,
            @JsonProperty("note") String note
    ) {}

    public record StrongAreaDto(
            @JsonProperty("category") String category,
            @JsonProperty("average_percentage") double averagePercentage,
            @JsonProperty("status") String status,
            @JsonProperty("note") String note
    ) {}
}
