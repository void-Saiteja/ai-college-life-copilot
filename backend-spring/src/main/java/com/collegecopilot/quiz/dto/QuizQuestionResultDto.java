package com.collegecopilot.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class QuizQuestionResultDto {

    private Integer id;
    private String question;
    private List<String> options = new ArrayList<>();
    private Integer selectedAnswer;
    private Integer correctAnswer;

    @JsonProperty("isCorrect")
    private boolean isCorrect;

    private String explanation;

    public QuizQuestionResultDto() {
    }

    public QuizQuestionResultDto(Integer id, String question, List<String> options, Integer selectedAnswer, Integer correctAnswer, boolean isCorrect, String explanation) {
        this.id = id;
        this.question = question;
        this.options = options != null ? options : new ArrayList<>();
        this.selectedAnswer = selectedAnswer;
        this.correctAnswer = correctAnswer;
        this.isCorrect = isCorrect;
        this.explanation = explanation;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options != null ? options : new ArrayList<>(); }

    public Integer getSelectedAnswer() { return selectedAnswer; }
    public void setSelectedAnswer(Integer selectedAnswer) { this.selectedAnswer = selectedAnswer; }

    public Integer getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(Integer correctAnswer) { this.correctAnswer = correctAnswer; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
