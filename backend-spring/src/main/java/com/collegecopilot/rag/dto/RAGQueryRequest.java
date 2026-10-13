package com.collegecopilot.rag.dto;

public class RAGQueryRequest {

    private String query;

    public RAGQueryRequest() {
    }

    public RAGQueryRequest(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
