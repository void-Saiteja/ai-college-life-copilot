package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record LegacyInterviewPracticeRequest(
        @JsonProperty("targetRole")
        @JsonAlias({"target_role"})
        String targetRole,

        @JsonProperty("question")
        String question,

        @JsonProperty("answer")
        String answer
) {}
