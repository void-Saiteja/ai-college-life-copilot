package com.collegecopilot.assignment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentSummaryDto {

    private int total;
    private int pending;
    private int completed;
    private int overdue;
    private int dueSoon;

    public AssignmentSummaryDto() {
    }

    public AssignmentSummaryDto(int total, int pending, int completed, int overdue, int dueSoon) {
        this.total = total;
        this.pending = pending;
        this.completed = completed;
        this.overdue = overdue;
        this.dueSoon = dueSoon;
    }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getPending() { return pending; }
    public void setPending(int pending) { this.pending = pending; }

    public int getCompleted() { return completed; }
    public void setCompleted(int completed) { this.completed = completed; }

    public int getOverdue() { return overdue; }
    public void setOverdue(int overdue) { this.overdue = overdue; }

    public int getDueSoon() { return dueSoon; }
    public void setDueSoon(int dueSoon) { this.dueSoon = dueSoon; }
}
