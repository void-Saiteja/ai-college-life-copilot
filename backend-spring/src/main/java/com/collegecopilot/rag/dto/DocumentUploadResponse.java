package com.collegecopilot.rag.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocumentUploadResponse {

    private boolean success;
    private String message;
    private DocumentDto data;

    public DocumentUploadResponse() {
    }

    public DocumentUploadResponse(boolean success, String message, DocumentDto data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DocumentDto getData() {
        return data;
    }

    public void setData(DocumentDto data) {
        this.data = data;
    }
}
