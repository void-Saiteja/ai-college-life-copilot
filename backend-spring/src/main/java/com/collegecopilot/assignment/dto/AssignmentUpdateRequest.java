package com.collegecopilot.assignment.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AssignmentUpdateRequest {

    private String title;
    private String description;
    private String course;

    @JsonProperty("subject_id")
    @JsonAlias({"subjectId", "subject_id"})
    private String subjectId;

    @JsonProperty("dueDate")
    @JsonAlias({"dueDate", "due_date"})
    private String dueDate;

    private String priority;
    private String status;

    @JsonProperty("estimatedHours")
    @JsonAlias({"estimatedHours", "estimated_hours"})
    private Double estimatedHours;

    private String category;

    public AssignmentUpdateRequest() {
    }

    public AssignmentUpdateRequest(String title, String description, String course, String subjectId,
                                   String dueDate, String priority, String status, Double estimatedHours, String category) {
        this.title = title;
        this.description = description;
        this.course = course;
        this.subjectId = subjectId;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status;
        this.estimatedHours = estimatedHours;
        this.category = category;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
