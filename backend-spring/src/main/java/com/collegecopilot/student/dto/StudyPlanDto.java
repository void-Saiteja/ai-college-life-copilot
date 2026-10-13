package com.collegecopilot.student.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StudyPlanDto {

    private Object plan;
    private List<Object> sessions;

    public StudyPlanDto() {
    }

    public StudyPlanDto(Object plan, List<Object> sessions) {
        this.plan = plan;
        this.sessions = sessions;
    }

    public Object getPlan() { return plan; }
    public void setPlan(Object plan) { this.plan = plan; }

    public List<Object> getSessions() { return sessions; }
    public void setSessions(List<Object> sessions) { this.sessions = sessions; }
}
