package com.collegecopilot.rag.dto;

import java.util.ArrayList;
import java.util.List;

public class RAGResultDto {

    private boolean found;
    private String answer;
    private List<RAGSourceDto> sources = new ArrayList<>();

    public RAGResultDto() {
    }

    public RAGResultDto(boolean found, String answer, List<RAGSourceDto> sources) {
        this.found = found;
        this.answer = answer;
        this.sources = sources != null ? sources : new ArrayList<>();
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<RAGSourceDto> getSources() {
        return sources;
    }

    public void setSources(List<RAGSourceDto> sources) {
        this.sources = sources != null ? sources : new ArrayList<>();
    }
}
