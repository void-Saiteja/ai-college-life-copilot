package com.collegecopilot.career.dto;

public class SkillGapItemDto {

    private String skill;
    private String studentProficiency;
    private String classification;

    public SkillGapItemDto() {
    }

    public SkillGapItemDto(String skill, String studentProficiency, String classification) {
        this.skill = skill;
        this.studentProficiency = studentProficiency;
        this.classification = classification;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public String getStudentProficiency() {
        return studentProficiency;
    }

    public void setStudentProficiency(String studentProficiency) {
        this.studentProficiency = studentProficiency;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }
}
