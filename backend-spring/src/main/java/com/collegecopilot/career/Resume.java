package com.collegecopilot.career;

import com.collegecopilot.career.dto.ResumeAnalysisDto;
import com.collegecopilot.career.dto.ResumeDto;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "student_id", length = 36, nullable = false)
    private String studentId;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "target_role", length = 100)
    private String targetRole = "Full Stack Developer";

    @Column(name = "file_name", length = 255)
    private String fileName = "Resume.pdf";

    @Column(name = "extracted_text", columnDefinition = "LONGTEXT", nullable = false)
    private String extractedText;

    @Convert(converter = ResumeAnalysisJsonConverter.class)
    @Column(name = "analysis", columnDefinition = "JSON")
    private ResumeAnalysisDto analysis;

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

    public Resume() {
    }

    public Resume(String id, String studentId, String title, String targetRole, String fileName,
                  String extractedText, ResumeAnalysisDto analysis) {
        this.id = id;
        this.studentId = studentId;
        this.title = title;
        this.targetRole = targetRole != null && !targetRole.isBlank() ? targetRole : "Full Stack Developer";
        this.fileName = fileName != null && !fileName.isBlank() ? fileName : "Resume.pdf";
        this.extractedText = extractedText;
        this.analysis = analysis;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public ResumeDto toDto() {
        return new ResumeDto(
                id,
                studentId,
                title,
                targetRole,
                fileName,
                extractedText,
                extractedText,
                createdAt != null ? createdAt.toString() : null,
                updatedAt != null ? updatedAt.toString() : null,
                analysis
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public void setTargetRole(String targetRole) {
        this.targetRole = targetRole;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public ResumeAnalysisDto getAnalysis() {
        return analysis;
    }

    public void setAnalysis(ResumeAnalysisDto analysis) {
        this.analysis = analysis;
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
