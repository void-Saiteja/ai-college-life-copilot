package com.collegecopilot.schedule.dto;

import java.util.List;

public class ScheduleListResponse {

    private boolean success;
    private List<ScheduleItemDto> data;

    public ScheduleListResponse() {
    }

    public ScheduleListResponse(boolean success, List<ScheduleItemDto> data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<ScheduleItemDto> getData() {
        return data;
    }

    public void setData(List<ScheduleItemDto> data) {
        this.data = data;
    }
}
