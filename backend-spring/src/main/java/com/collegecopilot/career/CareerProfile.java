package com.collegecopilot.career;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "career_profiles")
public class CareerProfile {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "student_id", length = 36, nullable = false, unique = true)
    private String studentId;

    @Column(name = "career_goal", length = 255)
    private String careerGoal = "Become a Software Engineer";

    @Column(name = "target_role", length = 100)
    private String targetRole = "Full Stack Developer";

    @Column(name = "domain", length = 100)
    private String domain = "Software Development";

    @Convert(converter = SkillListJsonConverter.class)
    @Column(name = "skills", columnDefinition = "JSON")
    private List<SkillItem> skills = new ArrayList<>();

    @Convert(converter = CareerStringListJsonConverter.class)
    @Column(name = "interests", columnDefinition = "JSON")
    private List<String> interests = new ArrayList<>();

    @Convert(converter = CareerObjectListJsonConverter.class)
    @Column(name = "experience_projects", columnDefinition = "JSON")
    private List<Object> experienceProjects = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public CareerProfile() {
    }

    public CareerProfile(String id, String studentId, String careerGoal, String targetRole, String domain,
                         List<SkillItem> skills, List<String> interests, List<Object> experienceProjects) {
        this.id = id;
        this.studentId = studentId;
        this.careerGoal = (careerGoal != null && !careerGoal.isBlank()) ? careerGoal : "Become a Software Engineer";
        this.targetRole = (targetRole != null && !targetRole.isBlank()) ? targetRole : "Full Stack Developer";
        this.domain = (domain != null && !domain.isBlank()) ? domain : "Software Development";
        this.skills = skills != null ? skills : new ArrayList<>();
        this.interests = interests != null ? interests : new ArrayList<>();
        this.experienceProjects = experienceProjects != null ? experienceProjects : new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
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

    public List<SkillItem> getSkills() {
        return skills;
    }

    public void setSkills(List<SkillItem> skills) {
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
