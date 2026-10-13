package com.collegecopilot.rag.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RAGSourceDto {

    private String documentTitle;
    private int pageNumber;
    private double similarityScore;
    private String snippet;

    public RAGSourceDto() {
    }

    public RAGSourceDto(String documentTitle, int pageNumber, double similarityScore, String snippet) {
        this.documentTitle = documentTitle;
        this.pageNumber = pageNumber;
        this.similarityScore = similarityScore;
        this.snippet = snippet;
    }

    public String getDocumentTitle() {
        return documentTitle;
    }

    public void setDocumentTitle(String documentTitle) {
        this.documentTitle = documentTitle;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
}
