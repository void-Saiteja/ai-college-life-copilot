package com.collegecopilot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceRecordDto {

    private String id;
    private String subjectId;
    private String subjectCode;
    private String subjectName;
    private String instructor;
    private int conducted;
    private int attended;
    private double percentage;
    private int targetPercentage;
    private int maxCanMiss;
    private int minRequiredToAttend;
    private String status;
    private String riskClassification;

    public AttendanceRecordDto() {
    }

    public AttendanceRecordDto(String id, String subjectId, String subjectCode, String subjectName, String instructor,
                               AttendanceMetrics metrics) {
        this.id = id;
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.instructor = instructor;
        if (metrics != null) {
            this.conducted = metrics.getConducted();
            this.attended = metrics.getAttended();
            this.percentage = metrics.getPercentage();
            this.targetPercentage = metrics.getTargetPercentage();
            this.maxCanMiss = metrics.getMaxCanMiss();
            this.minRequiredToAttend = metrics.getMinRequiredToAttend();
            this.status = metrics.getStatus();
            this.riskClassification = metrics.getRiskClassification();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getInstructor() { return instructor; }
    public void setInstructor(String instructor) { this.instructor = instructor; }

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
