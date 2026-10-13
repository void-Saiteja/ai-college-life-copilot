package com.collegecopilot.career;

import com.collegecopilot.career.dto.InterviewQuestionDto;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_questions")
public class InterviewQuestion {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "student_id", length = 36, nullable = false)
    private String studentId;

    @Column(name = "question", columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(name = "category", length = 50)
    private String category = "TECHNICAL";

    @Column(name = "difficulty", length = 50)
    private String difficulty = "MEDIUM";

    @Column(name = "target_skill", length = 100)
    private String targetSkill;

    @Column(name = "target_role", length = 100)
    private String targetRole = "Full Stack Developer";

    @Column(name = "preparation_tips", columnDefinition = "TEXT")
    private String preparationTips;

    @Column(name = "is_ai_generated")
    private Boolean isAiGenerated = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public InterviewQuestion() {
    }

    public InterviewQuestion(String id, String studentId, String question, String category,
                             String difficulty, String targetSkill, String targetRole,
                             String preparationTips, Boolean isAiGenerated) {
        this.id = id;
        this.studentId = studentId;
        this.question = question;
        this.category = category != null && !category.isBlank() ? category : "TECHNICAL";
        this.difficulty = difficulty != null && !difficulty.isBlank() ? difficulty : "MEDIUM";
        this.targetSkill = targetSkill;
        this.targetRole = targetRole != null && !targetRole.isBlank() ? targetRole : "Full Stack Developer";
        this.preparationTips = preparationTips;
        this.isAiGenerated = isAiGenerated != null ? isAiGenerated : true;
        this.createdAt = LocalDateTime.now();
    }

    public InterviewQuestionDto toDto() {
        return new InterviewQuestionDto(
                id,
                studentId,
                question,
                category,
                difficulty,
                targetSkill,
                targetRole,
                preparationTips,
                Boolean.TRUE.equals(isAiGenerated),
                createdAt != null ? createdAt.toString() : null
        );
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

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getTargetSkill() {
        return targetSkill;
    }

    public void setTargetSkill(String targetSkill) {
        this.targetSkill = targetSkill;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getPreparationTips() {
        return preparationTips;
    }

    public void setPreparationTips(String preparationTips) {
        this.preparationTips = preparationTips;
    }

    public Boolean getIsAiGenerated() {
        return isAiGenerated;
    }

    public void setIsAiGenerated(Boolean aiGenerated) {
        isAiGenerated = aiGenerated;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
