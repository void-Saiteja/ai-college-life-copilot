package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record InterviewFeedbackDto(
        @JsonProperty("strengths") List<String> strengths,
        @JsonProperty("improvements") List<String> improvements,
        @JsonProperty("overall_feedback") String overallFeedback,
        @JsonProperty("follow_up_tip") String followUpTip,
        @JsonProperty("isAiFeedback") boolean isAiFeedback
) {}
