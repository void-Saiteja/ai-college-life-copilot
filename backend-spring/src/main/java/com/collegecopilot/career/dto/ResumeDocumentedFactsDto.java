package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ResumeDocumentedFactsDto(
        @JsonProperty("targetRole") String targetRole,
        @JsonProperty("detectedSkillsCount") int detectedSkillsCount,
        @JsonProperty("coveragePercentage") double coveragePercentage,
        @JsonProperty("matchedRoleSkills") List<String> matchedRoleSkills,
        @JsonProperty("missingRoleSkills") List<String> missingRoleSkills
) {}
