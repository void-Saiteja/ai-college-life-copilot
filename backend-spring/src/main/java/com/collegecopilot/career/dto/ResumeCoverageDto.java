package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ResumeCoverageDto(
        @JsonProperty("targetRole") String targetRole,
        @JsonProperty("totalRequired") int totalRequired,
        @JsonProperty("matchedCount") int matchedCount,
        @JsonProperty("missingCount") int missingCount,
        @JsonProperty("coveragePercentage") double coveragePercentage,
        @JsonProperty("matchedSkills") List<String> matchedSkills,
        @JsonProperty("missingSkills") List<String> missingSkills,
        @JsonProperty("coverageFormula") String coverageFormula
) {}
