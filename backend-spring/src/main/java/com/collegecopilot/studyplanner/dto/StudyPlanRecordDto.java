package com.collegecopilot.studyplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudyPlanRecordDto {

    private String id;

    @JsonProperty("student_id")
    private String studentId;

    private String title;

    @JsonProperty("start_date")
    private String startDate;

    @JsonProperty("end_date")
    private String endDate;

    @JsonProperty("total_hours")
    private BigDecimal totalHours;

    private String status;

    @JsonProperty("ai_metadata")
    private StructuredPlanDto aiMetadata;

    public StudyPlanRecordDto() {
    }

    public StudyPlanRecordDto(String id, String studentId, String title, String startDate, String endDate, BigDecimal totalHours, String status, StructuredPlanDto aiMetadata) {
        this.id = id;
        this.studentId = studentId;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalHours = totalHours;
        this.status = status;
        this.aiMetadata = aiMetadata;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public BigDecimal getTotalHours() { return totalHours; }
    public void setTotalHours(BigDecimal totalHours) { this.totalHours = totalHours; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public StructuredPlanDto getAiMetadata() { return aiMetadata; }
    public void setAiMetadata(StructuredPlanDto aiMetadata) { this.aiMetadata = aiMetadata; }
}
