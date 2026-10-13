package com.collegecopilot.assignment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class AssignmentCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String course;

    @JsonProperty("subject_id")
    @JsonAlias({"subjectId", "subject_id"})
    private String subjectId;

    @JsonProperty("dueDate")
    @JsonAlias({"dueDate", "due_date"})
    private String dueDate;

    private String priority;

    @JsonProperty("estimatedHours")
    @JsonAlias({"estimatedHours", "estimated_hours"})
    private Double estimatedHours;

    private String category;
    private String description;

    public AssignmentCreateRequest() {
    }

    public AssignmentCreateRequest(String title, String course, String subjectId, String dueDate,
                                   String priority, Double estimatedHours, String category, String description) {
        this.title = title;
        this.course = course;
        this.subjectId = subjectId;
        this.dueDate = dueDate;
        this.priority = priority;
        this.estimatedHours = estimatedHours;
        this.category = category;
        this.description = description;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
