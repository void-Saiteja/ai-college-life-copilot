package com.collegecopilot.note.dto;

public class NoteSummarizeResponse {

    private boolean success;
    private NoteSummaryDataDto data;

    public NoteSummarizeResponse() {
    }

    public NoteSummarizeResponse(boolean success, NoteSummaryDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public NoteSummaryDataDto getData() {
        return data;
    }

    public void setData(NoteSummaryDataDto data) {
        this.data = data;
    }
}
