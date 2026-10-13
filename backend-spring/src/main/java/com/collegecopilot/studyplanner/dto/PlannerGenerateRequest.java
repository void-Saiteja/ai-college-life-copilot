package com.collegecopilot.studyplanner.dto;

import java.util.Arrays;
import java.util.List;

public class PlannerGenerateRequest {

    private String title = "Midterm Exam Sprint 2026";
    private Double availableHoursPerDay = 3.0;
    private String startTime = "16:00";
    private String endTime = "21:00";
    private List<String> studyDays = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
    private String focusSubjects = "";

    public PlannerGenerateRequest() {
    }

    public PlannerGenerateRequest(String title, Double availableHoursPerDay, String startTime, String endTime, List<String> studyDays, String focusSubjects) {
        if (title != null && !title.isBlank()) this.title = title;
        if (availableHoursPerDay != null) this.availableHoursPerDay = availableHoursPerDay;
        if (startTime != null && !startTime.isBlank()) this.startTime = startTime;
        if (endTime != null && !endTime.isBlank()) this.endTime = endTime;
        if (studyDays != null && !studyDays.isEmpty()) this.studyDays = studyDays;
        if (focusSubjects != null) this.focusSubjects = focusSubjects;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        if (title != null && !title.isBlank()) this.title = title;
    }

    public Double getAvailableHoursPerDay() { return availableHoursPerDay; }
    public void setAvailableHoursPerDay(Double availableHoursPerDay) {
        if (availableHoursPerDay != null) this.availableHoursPerDay = availableHoursPerDay;
    }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) {
        if (startTime != null && !startTime.isBlank()) this.startTime = startTime;
    }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) {
        if (endTime != null && !endTime.isBlank()) this.endTime = endTime;
    }

    public List<String> getStudyDays() { return studyDays; }
    public void setStudyDays(List<String> studyDays) {
        if (studyDays != null && !studyDays.isEmpty()) this.studyDays = studyDays;
    }

    public String getFocusSubjects() { return focusSubjects; }
    public void setFocusSubjects(String focusSubjects) {
        if (focusSubjects != null) this.focusSubjects = focusSubjects;
    }
}
