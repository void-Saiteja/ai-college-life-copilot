package com.collegecopilot.schedule.dto;

public class ScheduleCreateRequest {

    private String course;
    private String room;
    private Object days;
    private String time;
    private String instructor;

    public ScheduleCreateRequest() {
    }

    public ScheduleCreateRequest(String course, String room, Object days, String time, String instructor) {
        this.course = course;
        this.room = room;
        this.days = days;
        this.time = time;
        this.instructor = instructor;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public Object getDays() {
        return days;
    }

    public void setDays(Object days) {
        this.days = days;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }
}
