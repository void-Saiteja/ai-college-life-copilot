package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InterviewPracticeDto(
        @JsonProperty("id") String id,
        @JsonProperty("student_id") String studentId,
        @JsonProperty("question_id") String questionId,
        @JsonProperty("question") String question,
        @JsonProperty("category") String category,
        @JsonProperty("difficulty") String difficulty,
        @JsonProperty("target_skill") String targetSkill,
        @JsonProperty("target_role") String targetRole,
        @JsonProperty("answer") String answer,
        @JsonProperty("scores") InterviewScoresDto scores,
        @JsonProperty("feedback") InterviewFeedbackDto feedback,
        @JsonProperty("created_at") String createdAt
) {}
