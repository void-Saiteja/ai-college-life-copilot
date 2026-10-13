package com.collegecopilot.student;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "student_id", length = 36)
    private String studentId;

    @Column(name = "subject_id", length = 36, nullable = false)
    private String subjectId;

    @Column(name = "title", length = 150, nullable = false)
    private String title;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "location", length = 100)
    private String location = "Main Exam Hall";

    @Column(name = "syllabus", columnDefinition = "TEXT")
    private String syllabus;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private String status = "Upcoming";

    public Exam() {
    }

    public Exam(String id, String studentId, String subjectId, String title, LocalDate examDate, LocalTime startTime, String location) {
        this.id = id;
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.title = title;
        this.examDate = examDate;
        this.startTime = startTime;
        this.location = location != null ? location : "Main Exam Hall";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getExamDate() { return examDate; }
    public void setExamDate(LocalDate examDate) { this.examDate = examDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSyllabus() { return syllabus; }
    public void setSyllabus(String syllabus) { this.syllabus = syllabus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status != null ? status : "Upcoming"; }
    public void setStatus(String status) { this.status = status; }
}
