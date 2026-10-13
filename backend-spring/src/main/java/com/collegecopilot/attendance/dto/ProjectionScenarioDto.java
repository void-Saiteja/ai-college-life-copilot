package com.collegecopilot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectionScenarioDto {

    private Integer additionalMissed;
    private Integer additionalAttended;
    private int projectedConducted;
    private int projectedAttended;
    private double projectedPercentage;
    private String status;
    private String riskClassification;
    private int maxCanMiss;
    private int minRequiredToAttend;

    public ProjectionScenarioDto() {
    }

    public Integer getAdditionalMissed() { return additionalMissed; }
    public void setAdditionalMissed(Integer additionalMissed) { this.additionalMissed = additionalMissed; }

    public Integer getAdditionalAttended() { return additionalAttended; }
    public void setAdditionalAttended(Integer additionalAttended) { this.additionalAttended = additionalAttended; }

    public int getProjectedConducted() { return projectedConducted; }
    public void setProjectedConducted(int projectedConducted) { this.projectedConducted = projectedConducted; }

    public int getProjectedAttended() { return projectedAttended; }
    public void setProjectedAttended(int projectedAttended) { this.projectedAttended = projectedAttended; }

    public double getProjectedPercentage() { return projectedPercentage; }
    public void setProjectedPercentage(double projectedPercentage) { this.projectedPercentage = projectedPercentage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRiskClassification() { return riskClassification; }
    public void setRiskClassification(String riskClassification) { this.riskClassification = riskClassification; }

    public int getMaxCanMiss() { return maxCanMiss; }
    public void setMaxCanMiss(int maxCanMiss) { this.maxCanMiss = maxCanMiss; }

    public int getMinRequiredToAttend() { return minRequiredToAttend; }
    public void setMinRequiredToAttend(int minRequiredToAttend) { this.minRequiredToAttend = minRequiredToAttend; }
}
