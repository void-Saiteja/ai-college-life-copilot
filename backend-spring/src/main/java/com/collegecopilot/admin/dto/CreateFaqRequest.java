package com.collegecopilot.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateFaqRequest {

    @NotBlank(message = "Question and answer are required")
    private String question;

    @NotBlank(message = "Question and answer are required")
    private String answer;

    private String category = "Academic";

    public CreateFaqRequest() {
    }

    public CreateFaqRequest(String question, String answer, String category) {
        this.question = question;
        this.answer = answer;
        this.category = (category != null && !category.isBlank()) ? category : "Academic";
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
