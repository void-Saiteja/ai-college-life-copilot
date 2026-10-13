package com.collegecopilot.student.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceDto {

    private String subject;
    private String code;
    private int conducted;
    private int attended;
    private int percentage;
    private String status;

    public AttendanceDto() {
    }

    public AttendanceDto(String subject, String code, int conducted, int attended, int percentage, String status) {
        this.subject = subject;
        this.code = code;
        this.conducted = conducted;
        this.attended = attended;
        this.percentage = percentage;
        this.status = status;
    }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public int getConducted() { return conducted; }
    public void setConducted(int conducted) { this.conducted = conducted; }

    public int getAttended() { return attended; }
    public void setAttended(int attended) { this.attended = attended; }

    public int getPercentage() { return percentage; }
    public void setPercentage(int percentage) { this.percentage = percentage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
