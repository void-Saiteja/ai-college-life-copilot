package com.collegecopilot.rag.dto;

public class ExtractedPage {

    private int pageNumber;
    private String text;

    public ExtractedPage() {
    }

    public ExtractedPage(int pageNumber, String text) {
        this.pageNumber = pageNumber;
        this.text = text;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
