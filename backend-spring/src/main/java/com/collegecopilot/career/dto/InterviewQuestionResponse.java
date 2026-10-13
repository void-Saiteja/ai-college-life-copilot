package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InterviewQuestionResponse(
        @JsonProperty("success") boolean success,
        @JsonProperty("data") InterviewQuestionDto data
) {}
