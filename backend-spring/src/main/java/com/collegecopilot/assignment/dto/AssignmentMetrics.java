package com.collegecopilot.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentMetrics {

    private int daysRemaining;
    private String deadlineStatus;
    private String urgency;

    public AssignmentMetrics() {
    }

    public AssignmentMetrics(int daysRemaining, String deadlineStatus, String urgency) {
        this.daysRemaining = daysRemaining;
        this.deadlineStatus = deadlineStatus;
        this.urgency = urgency;
    }

    public int getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(int daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public String getDeadlineStatus() {
        return deadlineStatus;
    }

    public void setDeadlineStatus(String deadlineStatus) {
        this.deadlineStatus = deadlineStatus;
    }

    public String getUrgency() {
        return urgency;
    }

    public void setUrgency(String urgency) {
        this.urgency = urgency;
    }
}
