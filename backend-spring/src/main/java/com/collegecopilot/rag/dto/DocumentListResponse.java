package com.collegecopilot.rag.dto;

import java.util.ArrayList;
import java.util.List;

public class DocumentListResponse {

    private boolean success;
    private List<DocumentDto> data = new ArrayList<>();

    public DocumentListResponse() {
    }

    public DocumentListResponse(boolean success, List<DocumentDto> data) {
        this.success = success;
        this.data = data != null ? data : new ArrayList<>();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<DocumentDto> getData() {
        return data;
    }

    public void setData(List<DocumentDto> data) {
        this.data = data != null ? data : new ArrayList<>();
    }
}
