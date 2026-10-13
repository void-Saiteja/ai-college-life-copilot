package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record InterviewQuestionDto(
        @JsonProperty("id") String id,
        @JsonProperty("student_id") String studentId,
        @JsonProperty("question") String question,
        @JsonProperty("category") String category,
        @JsonProperty("difficulty") String difficulty,
        @JsonProperty("target_skill") String targetSkill,
        @JsonProperty("target_role") String targetRole,
        @JsonProperty("preparation_tips") String preparationTips,
        @JsonProperty("isAiGenerated")
        @JsonAlias({"is_ai_generated"})
        boolean isAiGenerated,
        @JsonProperty("created_at") String createdAt
) {}
