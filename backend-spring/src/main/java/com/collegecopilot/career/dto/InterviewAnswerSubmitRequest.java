package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InterviewAnswerSubmitRequest(
        @JsonProperty("answer")
        String answer
) {}
