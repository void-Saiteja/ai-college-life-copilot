package com.collegecopilot.career;

import com.collegecopilot.career.dto.InterviewFeedbackDto;
import com.collegecopilot.career.dto.InterviewPracticeDto;
import com.collegecopilot.career.dto.InterviewScoresDto;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "interview_practices")
public class InterviewPractice {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "student_id", length = 36, nullable = false)
    private String studentId;

    @Column(name = "question_id", length = 36)
    private String questionId;

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

    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Convert(converter = InterviewScoresJsonConverter.class)
    @Column(name = "scores", columnDefinition = "JSON")
    private InterviewScoresDto scores;

    @Convert(converter = InterviewFeedbackJsonConverter.class)
    @Column(name = "feedback", columnDefinition = "TEXT")
    private InterviewFeedbackDto feedback;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public InterviewPractice() {
    }

    public InterviewPractice(String id, String studentId, String questionId, String question,
                             String category, String difficulty, String targetSkill,
                             String targetRole, String answer, InterviewScoresDto scores,
                             InterviewFeedbackDto feedback) {
        this.id = id;
        this.studentId = studentId;
        this.questionId = questionId;
        this.question = question;
        this.category = category != null && !category.isBlank() ? category : "TECHNICAL";
        this.difficulty = difficulty != null && !difficulty.isBlank() ? difficulty : "MEDIUM";
        this.targetSkill = targetSkill;
        this.targetRole = targetRole != null && !targetRole.isBlank() ? targetRole : "Full Stack Developer";
        this.answer = answer;
        this.scores = scores;
        this.feedback = feedback;
        this.createdAt = LocalDateTime.now();
    }

    public InterviewPracticeDto toDto() {
        return new InterviewPracticeDto(
                id,
                studentId,
                questionId,
                question,
                category,
                difficulty,
                targetSkill,
                targetRole,
                answer,
                scores,
                feedback,
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

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
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

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public InterviewScoresDto getScores() {
        return scores;
    }

    public void setScores(InterviewScoresDto scores) {
        this.scores = scores;
    }

    public InterviewFeedbackDto getFeedback() {
        return feedback;
    }

    public void setFeedback(InterviewFeedbackDto feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
