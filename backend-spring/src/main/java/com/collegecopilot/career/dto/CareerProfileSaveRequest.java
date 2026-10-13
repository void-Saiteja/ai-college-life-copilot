package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CareerProfileSaveRequest {

    @JsonProperty("career_goal")
    private String careerGoal;

    @JsonProperty("target_role")
    private String targetRole;

    private String domain;

    private List<SkillItemDto> skills;

    private List<String> interests;

    @JsonProperty("experience_projects")
    private List<Object> experienceProjects;

    public CareerProfileSaveRequest() {
    }

    public CareerProfileSaveRequest(String careerGoal, String targetRole, String domain,
                                    List<SkillItemDto> skills, List<String> interests,
                                    List<Object> experienceProjects) {
        this.careerGoal = careerGoal;
        this.targetRole = targetRole;
        this.domain = domain;
        this.skills = skills;
        this.interests = interests;
        this.experienceProjects = experienceProjects;
    }

    public String getCareerGoal() {
        return careerGoal;
    }

    public void setCareerGoal(String careerGoal) {
        this.careerGoal = careerGoal;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<SkillItemDto> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillItemDto> skills) {
        this.skills = skills;
    }

    public List<String> getInterests() {
        return interests;
    }

    public void setInterests(List<String> interests) {
        this.interests = interests;
    }

    public List<Object> getExperienceProjects() {
        return experienceProjects;
    }

    public void setExperienceProjects(List<Object> experienceProjects) {
        this.experienceProjects = experienceProjects;
    }
}
