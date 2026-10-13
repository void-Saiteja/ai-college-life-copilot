package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InterviewHistoryResponse(
        @JsonProperty("success") boolean success,
        @JsonProperty("data") InterviewHistoryDataDto data
) {}
