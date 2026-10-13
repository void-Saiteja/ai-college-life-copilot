package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LegacyInterviewPracticeResponse(
        @JsonProperty("success") boolean success,
        @JsonProperty("data") LegacyInterviewPracticeDataDto data
) {
    public record LegacyInterviewPracticeDataDto(
            @JsonProperty("targetRole") String targetRole,
            @JsonProperty("question") String question,
            @JsonProperty("evaluatedAnswer") String evaluatedAnswer,
            @JsonProperty("clarityScore") int clarityScore,
            @JsonProperty("feedback") String feedback,
            @JsonProperty("followUpQuestion") String followUpQuestion
    ) {}
}
