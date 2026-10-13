package com.collegecopilot.attendance.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceListResponse {

    private boolean success = true;
    private List<AttendanceRecordDto> data;

    public AttendanceListResponse() {
    }

    public AttendanceListResponse(boolean success, List<AttendanceRecordDto> data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public List<AttendanceRecordDto> getData() { return data; }
    public void setData(List<AttendanceRecordDto> data) { this.data = data; }
}
