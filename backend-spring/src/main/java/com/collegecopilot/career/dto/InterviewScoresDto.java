package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InterviewScoresDto(
        @JsonProperty("relevance") double relevance,
        @JsonProperty("clarity") double clarity,
        @JsonProperty("completeness") double completeness,
        @JsonProperty("technical_understanding") double technicalUnderstanding,
        @JsonProperty("total") double total,
        @JsonProperty("max_total") double maxTotal,
        @JsonProperty("percentage") double percentage,
        @JsonProperty("score_formula") String scoreFormula
) {}
