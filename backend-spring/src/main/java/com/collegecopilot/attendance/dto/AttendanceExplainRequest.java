package com.collegecopilot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AttendanceExplainRequest {

    @JsonProperty("subjectId")
    @JsonAlias({"subjectId", "subject_id"})
    private String subjectId;

    private Integer conducted = 0;
    private Integer attended = 0;

    @JsonProperty("targetPercentage")
    @JsonAlias({"targetPercentage", "target_percentage", "target"})
    private Integer targetPercentage = 75;

    public AttendanceExplainRequest() {
    }

    public AttendanceExplainRequest(String subjectId, Integer conducted, Integer attended, Integer targetPercentage) {
        this.subjectId = subjectId;
        this.conducted = conducted != null ? conducted : 0;
        this.attended = attended != null ? attended : 0;
        this.targetPercentage = targetPercentage != null ? targetPercentage : 75;
    }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public Integer getConducted() { return conducted != null ? conducted : 0; }
    public void setConducted(Integer conducted) { this.conducted = conducted; }

    public Integer getAttended() { return attended != null ? attended : 0; }
    public void setAttended(Integer attended) { this.attended = attended; }

    public Integer getTargetPercentage() { return targetPercentage != null ? targetPercentage : 75; }
    public void setTargetPercentage(Integer targetPercentage) { this.targetPercentage = targetPercentage; }
}
