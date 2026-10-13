package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record LegacyResumeAnalyzeRequest(
        @JsonProperty("resumeText")
        @JsonAlias({"resume_text", "text", "extracted_text"})
        String resumeText,

        @JsonProperty("targetRole")
        @JsonAlias({"target_role"})
        String targetRole
) {}
