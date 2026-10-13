package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ResumeAnalysisDto(
        @JsonProperty("targetRole") String targetRole,
        @JsonProperty("coverage") ResumeCoverageDto coverage,
        @JsonProperty("detectedSkills") List<ResumeDetectedSkillDto> detectedSkills,
        @JsonProperty("careerSkillGaps") SkillGapAnalysisDto careerSkillGaps,
        @JsonProperty("documentedFacts") ResumeDocumentedFactsDto documentedFacts,
        @JsonProperty("aiSuggestions") ResumeAiSuggestionsDto aiSuggestions
) {}
