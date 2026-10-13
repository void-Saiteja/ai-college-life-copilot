package com.collegecopilot.studyplanner.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyTaskDto {

    @JsonProperty("start_time")
    private String startTime;

    @JsonProperty("end_time")
    private String endTime;

    private String subject;
    private String activity;
    private String reason;
    private String priority;

    public DailyTaskDto() {
    }

    public DailyTaskDto(String startTime, String endTime, String subject, String activity, String reason, String priority) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.subject = subject;
        this.activity = activity;
        this.reason = reason;
        this.priority = priority;
    }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getActivity() { return activity; }
    public void setActivity(String activity) { this.activity = activity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}
