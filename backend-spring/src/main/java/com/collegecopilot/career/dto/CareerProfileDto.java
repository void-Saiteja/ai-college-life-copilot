package com.collegecopilot.career.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class CareerProfileDto {

    private String id;

    @JsonProperty("student_id")
    private String studentId;

    @JsonProperty("career_goal")
    private String careerGoal;

    @JsonProperty("target_role")
    private String targetRole;

    private String domain;

    private List<SkillItemDto> skills = new ArrayList<>();

    private List<String> interests = new ArrayList<>();

    @JsonProperty("experience_projects")
    private List<Object> experienceProjects = new ArrayList<>();

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    public CareerProfileDto() {
    }

    public CareerProfileDto(String id, String studentId, String careerGoal, String targetRole, String domain,
                            List<SkillItemDto> skills, List<String> interests, List<Object> experienceProjects,
                            String createdAt, String updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.careerGoal = careerGoal;
        this.targetRole = targetRole;
        this.domain = domain;
        this.skills = skills != null ? skills : new ArrayList<>();
        this.interests = interests != null ? interests : new ArrayList<>();
        this.experienceProjects = experienceProjects != null ? experienceProjects : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
