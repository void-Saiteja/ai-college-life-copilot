package com.collegecopilot.note.dto;

import java.util.ArrayList;
import java.util.List;

public class NoteListResponse {

    private boolean success;
    private List<NoteDto> data = new ArrayList<>();

    public NoteListResponse() {
    }

    public NoteListResponse(boolean success, List<NoteDto> data) {
        this.success = success;
        this.data = data != null ? data : new ArrayList<>();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<NoteDto> getData() {
        return data;
    }

    public void setData(List<NoteDto> data) {
        this.data = data != null ? data : new ArrayList<>();
    }
}
