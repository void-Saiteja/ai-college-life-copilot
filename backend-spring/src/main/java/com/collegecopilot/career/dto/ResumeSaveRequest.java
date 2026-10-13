package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ResumeSaveRequest(
        @JsonProperty("title")
        String title,

        @JsonProperty("resume_text")
        @JsonAlias({"resumeText", "extracted_text", "extractedText"})
        String resumeText,

        @JsonProperty("target_role")
        @JsonAlias({"targetRole"})
        String targetRole
) {}
