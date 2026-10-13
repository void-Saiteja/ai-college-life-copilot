package com.collegecopilot.note.dto;

public class NoteResponse {

    private boolean success;
    private NoteDto data;

    public NoteResponse() {
    }

    public NoteResponse(boolean success, NoteDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public NoteDto getData() {
        return data;
    }

    public void setData(NoteDto data) {
        this.data = data;
    }
}
