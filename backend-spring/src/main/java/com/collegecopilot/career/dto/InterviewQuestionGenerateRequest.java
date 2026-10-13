package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record InterviewQuestionGenerateRequest(
        @JsonProperty("targetRole")
        @JsonAlias({"target_role"})
        String targetRole,

        @JsonProperty("category")
        String category,

        @JsonProperty("difficulty")
        String difficulty,

        @JsonProperty("skill")
        @JsonAlias({"target_skill", "targetSkill"})
        String skill
) {}
