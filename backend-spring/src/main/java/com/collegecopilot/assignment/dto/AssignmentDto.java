package com.collegecopilot.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentDto {

    private String id;
    private String studentId;
    private String subjectId;
    private String course;
    private String subjectName;
    private String title;
    private String description;
    private String dueDate;
    private String status;
    private String priority;
    private Double estimatedHours;
    private String category;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("completed_at")
    private String completedAt;

    private int daysRemaining;
    private String deadlineStatus;
    private String urgency;

    public AssignmentDto() {
    }

    public AssignmentDto(String id, String studentId, String subjectId, String course, String subjectName,
                         String title, String description, String dueDate, String status, String priority,
                         Double estimatedHours, String category, String createdAt, String completedAt,
                         AssignmentMetrics metrics) {
        this.id = id;
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.course = course;
        this.subjectName = subjectName;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
        this.estimatedHours = estimatedHours;
        this.category = category;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        if (metrics != null) {
            this.daysRemaining = metrics.getDaysRemaining();
            this.deadlineStatus = metrics.getDeadlineStatus();
            this.urgency = metrics.getUrgency();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Double getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Double estimatedHours) { this.estimatedHours = estimatedHours; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getCompletedAt() { return completedAt; }
    public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }

    public int getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(int daysRemaining) { this.daysRemaining = daysRemaining; }

    public String getDeadlineStatus() { return deadlineStatus; }
    public void setDeadlineStatus(String deadlineStatus) { this.deadlineStatus = deadlineStatus; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
}
