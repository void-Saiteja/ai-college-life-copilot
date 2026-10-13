package com.collegecopilot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceSingleResponse {

    private boolean success = true;
    private AttendanceRecordDto data;

    public AttendanceSingleResponse() {
    }

    public AttendanceSingleResponse(boolean success, AttendanceRecordDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public AttendanceRecordDto getData() { return data; }
    public void setData(AttendanceRecordDto data) { this.data = data; }
}
