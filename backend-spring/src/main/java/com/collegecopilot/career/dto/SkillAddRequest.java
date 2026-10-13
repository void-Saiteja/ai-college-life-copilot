package com.collegecopilot.career.dto;

public class SkillAddRequest {

    private String skill;
    private String proficiency = "BEGINNER";

    public SkillAddRequest() {
    }

    public SkillAddRequest(String skill, String proficiency) {
        this.skill = skill;
        this.proficiency = proficiency;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getProficiency() {
        return proficiency;
    }

    public void setProficiency(String proficiency) {
        this.proficiency = proficiency;
    }
}
