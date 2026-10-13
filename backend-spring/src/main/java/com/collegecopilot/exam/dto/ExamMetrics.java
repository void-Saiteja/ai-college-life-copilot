package com.collegecopilot.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExamMetrics {

    private int daysRemaining;
    private String examStatus;
    private String urgency;

    public ExamMetrics() {
    }

    public ExamMetrics(int daysRemaining, String examStatus, String urgency) {
        this.daysRemaining = daysRemaining;
        this.examStatus = examStatus;
        this.urgency = urgency;
    }

    public int getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(int daysRemaining) { this.daysRemaining = daysRemaining; }

    public String getExamStatus() { return examStatus; }
    public void setExamStatus(String examStatus) { this.examStatus = examStatus; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
}
