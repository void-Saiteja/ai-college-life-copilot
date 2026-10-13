package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ResumeListResponse(
        @JsonProperty("success") boolean success,
        @JsonProperty("data") List<ResumeDto> data
) {}
