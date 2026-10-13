package com.collegecopilot.student;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "student_id", length = 36, nullable = false)
    private String studentId;

    @Column(name = "subject_id", length = 36, nullable = false)
    private String subjectId;

    @Column(name = "classes_conducted")
    private Integer classesConducted = 0;

    @Column(name = "classes_attended")
    private Integer classesAttended = 0;

    @Column(name = "percentage", insertable = false, updatable = false)
    private BigDecimal percentage;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Attendance() {
    }

    public Attendance(String id, String studentId, String subjectId, Integer classesConducted, Integer classesAttended) {
        this.id = id;
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.classesConducted = classesConducted;
        this.classesAttended = classesAttended;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public Integer getClassesConducted() { return classesConducted != null ? classesConducted : 0; }
    public void setClassesConducted(Integer classesConducted) { this.classesConducted = classesConducted; }

    public Integer getClassesAttended() { return classesAttended != null ? classesAttended : 0; }
    public void setClassesAttended(Integer classesAttended) { this.classesAttended = classesAttended; }

    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
