package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ResumeDto(
        @JsonProperty("id") String id,
        @JsonProperty("student_id") String studentId,
        @JsonProperty("title") String title,
        @JsonProperty("target_role") String targetRole,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("resume_text") String resumeText,
        @JsonProperty("extracted_text") String extractedText,
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("updated_at") String updatedAt,
        @JsonProperty("analysis") ResumeAnalysisDto analysis
) {}
