package com.collegecopilot.career.dto;

import java.util.ArrayList;
import java.util.List;

public class RoadmapPhaseDto {

    private String phase;
    private List<String> skills = new ArrayList<>();
    private String priority;
    private String focus;
    private String effort;

    public RoadmapPhaseDto() {
    }

    public RoadmapPhaseDto(String phase, List<String> skills, String priority, String focus, String effort) {
        this.phase = phase;
        this.skills = skills != null ? skills : new ArrayList<>();
        this.priority = priority;
        this.focus = focus;
        this.effort = effort;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getFocus() {
        return focus;
    }

    public void setFocus(String focus) {
        this.focus = focus;
    }

    public String getEffort() {
        return effort;
    }

    public void setEffort(String effort) {
        this.effort = effort;
    }
}
