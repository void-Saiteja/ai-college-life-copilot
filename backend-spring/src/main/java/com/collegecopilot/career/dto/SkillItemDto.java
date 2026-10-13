package com.collegecopilot.career.dto;

public class SkillItemDto {

    private String skill;
    private String proficiency;

    public SkillItemDto() {
    }

    public SkillItemDto(String skill, String proficiency) {
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
