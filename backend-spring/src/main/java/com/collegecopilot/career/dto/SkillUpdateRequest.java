package com.collegecopilot.career.dto;

public class SkillUpdateRequest {

    private String proficiency;

    public SkillUpdateRequest() {
    }

    public SkillUpdateRequest(String proficiency) {
        this.proficiency = proficiency;
    }

    public String getProficiency() {
        return proficiency;
    }

    public void setProficiency(String proficiency) {
        this.proficiency = proficiency;
    }
}
