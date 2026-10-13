package com.collegecopilot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceMetrics {

    private int conducted;
    private int attended;
    private double percentage;
    private int targetPercentage;
    private int maxCanMiss;
    private int minRequiredToAttend;
    private String status;
    private String riskClassification;

    public AttendanceMetrics() {
    }

    public AttendanceMetrics(int conducted, int attended, double percentage, int targetPercentage,
                             int maxCanMiss, int minRequiredToAttend, String status, String riskClassification) {
        this.conducted = conducted;
        this.attended = attended;
        this.percentage = percentage;
        this.targetPercentage = targetPercentage;
        this.maxCanMiss = maxCanMiss;
        this.minRequiredToAttend = minRequiredToAttend;
        this.status = status;
        this.riskClassification = riskClassification;
    }

    public int getConducted() { return conducted; }
    public void setConducted(int conducted) { this.conducted = conducted; }

    public int getAttended() { return attended; }
    public void setAttended(int attended) { this.attended = attended; }

    public double getPercentage() { return percentage; }
    public void setPercentage(double percentage) { this.percentage = percentage; }

    public int getTargetPercentage() { return targetPercentage; }
    public void setTargetPercentage(int targetPercentage) { this.targetPercentage = targetPercentage; }

    public int getMaxCanMiss() { return maxCanMiss; }
    public void setMaxCanMiss(int maxCanMiss) { this.maxCanMiss = maxCanMiss; }

    public int getMinRequiredToAttend() { return minRequiredToAttend; }
    public void setMinRequiredToAttend(int minRequiredToAttend) { this.minRequiredToAttend = minRequiredToAttend; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRiskClassification() { return riskClassification; }
    public void setRiskClassification(String riskClassification) { this.riskClassification = riskClassification; }
}
