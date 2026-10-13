package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LegacyResumeAnalyzeResponse(
        @JsonProperty("success") boolean success,
        @JsonProperty("data") LegacyResumeAnalyzeDataDto data
) {
    public record LegacyResumeAnalyzeDataDto(
            @JsonProperty("targetRole") String targetRole,
            @JsonProperty("skillGapAnalysis") SkillGapAnalysisDto skillGapAnalysis
    ) {}
}
