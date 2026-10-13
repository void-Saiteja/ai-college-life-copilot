package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResumeDetectedSkillDto(
        @JsonProperty("skill") String skill,
        @JsonProperty("status") String status
) {
    public ResumeDetectedSkillDto(String skill) {
        this(skill, "DETECTED_IN_RESUME");
    }
}
