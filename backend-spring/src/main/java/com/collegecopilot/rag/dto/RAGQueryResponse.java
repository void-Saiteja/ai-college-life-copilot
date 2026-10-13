package com.collegecopilot.rag.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RAGQueryResponse {

    private boolean success;
    private RAGResultDto data;

    public RAGQueryResponse() {
    }

    public RAGQueryResponse(boolean success, RAGResultDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public RAGResultDto getData() {
        return data;
    }

    public void setData(RAGResultDto data) {
        this.data = data;
    }
}
