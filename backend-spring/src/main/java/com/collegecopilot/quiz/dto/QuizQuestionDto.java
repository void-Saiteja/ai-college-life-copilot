package com.collegecopilot.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuizQuestionDto {

    private Integer id;
    private String question;
    private List<String> options = new ArrayList<>();
    private Integer correctAnswer;
    private String explanation;

    public QuizQuestionDto() {
    }

    public QuizQuestionDto(Integer id, String question, List<String> options) {
        this.id = id;
        this.question = question;
        this.options = options != null ? options : new ArrayList<>();
    }

    public QuizQuestionDto(Integer id, String question, List<String> options, Integer correctAnswer, String explanation) {
        this.id = id;
        this.question = question;
        this.options = options != null ? options : new ArrayList<>();
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options != null ? options : new ArrayList<>(); }

    public Integer getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(Integer correctAnswer) { this.correctAnswer = correctAnswer; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
