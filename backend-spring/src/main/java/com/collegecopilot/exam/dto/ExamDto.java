package com.collegecopilot.exam.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExamDto {

    private String id;
    private String studentId;
    private String subjectId;
    private String subjectCode;
    private String subjectName;
    private String title;

    @JsonProperty("exam_date")
    private String examDate;

    @JsonProperty("start_time")
    private String startTime;

    private String location;
    private String syllabus;
    private String status;

    @JsonProperty("created_at")
    private String createdAt;

    private int daysRemaining;
    private String examStatus;
    private String urgency;

    public ExamDto() {
    }

    public ExamDto(String id, String studentId, String subjectId, String subjectCode, String subjectName,
                   String title, String examDate, String startTime, String location, String syllabus,
                   String status, String createdAt, ExamMetrics metrics) {
        this.id = id;
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.title = title;
        this.examDate = examDate;
        this.startTime = startTime;
        this.location = location;
        this.syllabus = syllabus;
        this.status = status;
        this.createdAt = createdAt;
        if (metrics != null) {
            this.daysRemaining = metrics.getDaysRemaining();
            this.examStatus = metrics.getExamStatus();
            this.urgency = metrics.getUrgency();
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getExamDate() { return examDate; }
    public void setExamDate(String examDate) { this.examDate = examDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSyllabus() { return syllabus; }
    public void setSyllabus(String syllabus) { this.syllabus = syllabus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getDaysRemaining() { return daysRemaining; }
    public void setDaysRemaining(int daysRemaining) { this.daysRemaining = daysRemaining; }

    public String getExamStatus() { return examStatus; }
    public void setExamStatus(String examStatus) { this.examStatus = examStatus; }

    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
}
