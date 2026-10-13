package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ResumeAiSuggestionsDto(
        @JsonProperty("strengths") List<String> strengths,
        @JsonProperty("improvements") List<String> improvements,
        @JsonProperty("actionableSuggestions") List<String> actionableSuggestions,
        @JsonProperty("isAiGenerated") boolean isAiGenerated,
        @JsonProperty("disclaimer") String disclaimer
) {}
