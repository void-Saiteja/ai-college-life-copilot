package com.collegecopilot.schedule.dto;

public class ScheduleSingleResponse {

    private boolean success;
    private ScheduleItemDto data;

    public ScheduleSingleResponse() {
    }

    public ScheduleSingleResponse(boolean success, ScheduleItemDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ScheduleItemDto getData() {
        return data;
    }

    public void setData(ScheduleItemDto data) {
        this.data = data;
    }
}
