package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InterviewPracticeResponse(
        @JsonProperty("success") boolean success,
        @JsonProperty("data") InterviewPracticeDto data
) {}
