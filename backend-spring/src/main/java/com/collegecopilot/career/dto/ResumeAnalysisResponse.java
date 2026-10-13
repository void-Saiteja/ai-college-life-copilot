package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResumeAnalysisResponse(
        @JsonProperty("success") boolean success,
        @JsonProperty("data") ResumeAnalysisDto data
) {}
