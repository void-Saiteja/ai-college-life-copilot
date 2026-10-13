package com.collegecopilot.student;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false, unique = true)
    private String userId;

    @Column(name = "student_code", length = 50, nullable = false, unique = true)
    private String studentCode;

    @Column(name = "semester")
    private Integer semester = 1;

    @Column(name = "department", length = 100)
    private String department = "Computer Science";

    @Column(name = "gpa", precision = 3, scale = 2)
    private BigDecimal gpa = new BigDecimal("3.80");

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Student() {
    }

    public Student(String id, String userId, String studentCode, Integer semester, String department, BigDecimal gpa) {
        this.id = id;
        this.userId = userId;
        this.studentCode = studentCode;
        this.semester = semester != null ? semester : 1;
        this.department = department != null ? department : "Computer Science";
        this.gpa = gpa != null ? gpa : new BigDecimal("3.80");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStudentCode() {
        return studentCode;
    }

    public void setStudentCode(String studentCode) {
        this.studentCode = studentCode;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public BigDecimal getGpa() {
        return gpa;
    }

    public void setGpa(BigDecimal gpa) {
        this.gpa = gpa;
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
