package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResumeSingleResponse(
        @JsonProperty("success") boolean success,
        @JsonProperty("data") ResumeDto data
) {}
