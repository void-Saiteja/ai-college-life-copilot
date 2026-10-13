package com.collegecopilot.career;

import java.util.Objects;

public class SkillItem {

    private String skill;
    private String proficiency = "BEGINNER";

    public SkillItem() {
    }

    public SkillItem(String skill, String proficiency) {
        this.skill = skill;
        this.proficiency = (proficiency != null && !proficiency.isBlank())
                ? proficiency.trim().toUpperCase()
                : "BEGINNER";
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
        this.proficiency = (proficiency != null && !proficiency.isBlank())
                ? proficiency.trim().toUpperCase()
                : "BEGINNER";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkillItem skillItem = (SkillItem) o;
        return Objects.equals(skill != null ? skill.toLowerCase().trim() : null,
                skillItem.skill != null ? skillItem.skill.toLowerCase().trim() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skill != null ? skill.toLowerCase().trim() : null);
    }
}
