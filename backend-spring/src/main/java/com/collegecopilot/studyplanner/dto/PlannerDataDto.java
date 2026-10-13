package com.collegecopilot.studyplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlannerDataDto {

    private StudyPlanRecordDto plan;
    private List<StudySessionDto> sessions = new ArrayList<>();
    private StructuredPlanDto structuredPlan;

    public PlannerDataDto() {
    }

    public PlannerDataDto(StudyPlanRecordDto plan, List<StudySessionDto> sessions, StructuredPlanDto structuredPlan) {
        this.plan = plan;
        this.sessions = sessions != null ? sessions : new ArrayList<>();
        this.structuredPlan = structuredPlan;
    }

    public StudyPlanRecordDto getPlan() { return plan; }
    public void setPlan(StudyPlanRecordDto plan) { this.plan = plan; }

    public List<StudySessionDto> getSessions() { return sessions; }
    public void setSessions(List<StudySessionDto> sessions) { this.sessions = sessions; }

    public StructuredPlanDto getStructuredPlan() { return structuredPlan; }
    public void setStructuredPlan(StructuredPlanDto structuredPlan) { this.structuredPlan = structuredPlan; }
}
