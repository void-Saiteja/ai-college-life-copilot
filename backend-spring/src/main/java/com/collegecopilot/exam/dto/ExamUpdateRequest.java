package com.collegecopilot.exam.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ExamUpdateRequest {

    private String title;

    @JsonProperty("exam_date")
    @JsonAlias({"examDate", "exam_date"})
    private String examDate;

    @JsonProperty("subject_id")
    @JsonAlias({"subjectId", "subject_id"})
    private String subjectId;

    @JsonProperty("start_time")
    @JsonAlias({"startTime", "start_time"})
    private String startTime;

    private String location;
    private String syllabus;
    private String status;

    public ExamUpdateRequest() {
    }

    public ExamUpdateRequest(String title, String examDate, String subjectId, String startTime,
                             String location, String syllabus, String status) {
        this.title = title;
        this.examDate = examDate;
        this.subjectId = subjectId;
        this.startTime = startTime;
        this.location = location;
        this.syllabus = syllabus;
        this.status = status;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getExamDate() { return examDate; }
    public void setExamDate(String examDate) { this.examDate = examDate; }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getSyllabus() { return syllabus; }
    public void setSyllabus(String syllabus) { this.syllabus = syllabus; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
