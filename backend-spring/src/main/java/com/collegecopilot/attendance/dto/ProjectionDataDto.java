package com.collegecopilot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectionDataDto {

    private AttendanceMetrics current;
    private ProjectionScenarioDto ifMissNext;
    private ProjectionScenarioDto ifAttendNext;
    private Map<String, Object> fullProjection;
    private String disclaimer = "Note: These calculations are mathematical projections for academic planning.";

    public ProjectionDataDto() {
    }

    public AttendanceMetrics getCurrent() { return current; }
    public void setCurrent(AttendanceMetrics current) { this.current = current; }

    public ProjectionScenarioDto getIfMissNext() { return ifMissNext; }
    public void setIfMissNext(ProjectionScenarioDto ifMissNext) { this.ifMissNext = ifMissNext; }

    public ProjectionScenarioDto getIfAttendNext() { return ifAttendNext; }
    public void setIfAttendNext(ProjectionScenarioDto ifAttendNext) { this.ifAttendNext = ifAttendNext; }

    public Map<String, Object> getFullProjection() { return fullProjection; }
    public void setFullProjection(Map<String, Object> fullProjection) { this.fullProjection = fullProjection; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
}
