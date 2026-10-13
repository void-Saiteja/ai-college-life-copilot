package com.collegecopilot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AttendanceProjectionRequest {

    private Integer conducted = 0;
    private Integer attended = 0;

    @JsonProperty("missClasses")
    @JsonAlias({"missClasses", "futureMissed", "miss_classes"})
    private Integer missClasses = 0;

    @JsonProperty("attendClasses")
    @JsonAlias({"attendClasses", "futureAttended", "attend_classes"})
    private Integer attendClasses = 0;

    @JsonProperty("targetPercentage")
    @JsonAlias({"targetPercentage", "target_percentage", "target"})
    private Integer targetPercentage = 75;

    public AttendanceProjectionRequest() {
    }

    public AttendanceProjectionRequest(Integer conducted, Integer attended, Integer missClasses, Integer attendClasses, Integer targetPercentage) {
        this.conducted = conducted != null ? conducted : 0;
        this.attended = attended != null ? attended : 0;
        this.missClasses = missClasses != null ? missClasses : 0;
        this.attendClasses = attendClasses != null ? attendClasses : 0;
        this.targetPercentage = targetPercentage != null ? targetPercentage : 75;
    }

    public Integer getConducted() { return conducted != null ? conducted : 0; }
    public void setConducted(Integer conducted) { this.conducted = conducted; }

    public Integer getAttended() { return attended != null ? attended : 0; }
    public void setAttended(Integer attended) { this.attended = attended; }

    public Integer getMissClasses() { return missClasses != null ? missClasses : 0; }
    public void setMissClasses(Integer missClasses) { this.missClasses = missClasses; }

    public Integer getAttendClasses() { return attendClasses != null ? attendClasses : 0; }
    public void setAttendClasses(Integer attendClasses) { this.attendClasses = attendClasses; }

    public Integer getTargetPercentage() { return targetPercentage != null ? targetPercentage : 75; }
    public void setTargetPercentage(Integer targetPercentage) { this.targetPercentage = targetPercentage; }
}
