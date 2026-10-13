package com.collegecopilot.studyplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlanPriorityDto {

    private String subject;
    private String reason;
    private String priority;

    public PlanPriorityDto() {
    }

    public PlanPriorityDto(String subject, String reason, String priority) {
        this.subject = subject;
        this.reason = reason;
        this.priority = priority;
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
