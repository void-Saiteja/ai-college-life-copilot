package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResumeDeleteResponse(
        @JsonProperty("success") boolean success,
        @JsonProperty("message") String message
) {}
