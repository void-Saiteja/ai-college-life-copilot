package com.collegecopilot.quiz.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizSubmitRequest {

    private String quizId;
    private Map<String, Object> answers = new HashMap<>();
    private List<QuizQuestionDto> rawQuestions;

    public QuizSubmitRequest() {
    }

    public QuizSubmitRequest(Map<String, Object> answers, List<QuizQuestionDto> rawQuestions) {
        this(null, answers, rawQuestions);
    }

    public QuizSubmitRequest(String quizId, Map<String, Object> answers, List<QuizQuestionDto> rawQuestions) {
        this.quizId = quizId;
        this.answers = answers != null ? answers : new HashMap<>();
        this.rawQuestions = rawQuestions;
    }

    public String getQuizId() { return quizId; }
    public void setQuizId(String quizId) { this.quizId = quizId; }

    public Map<String, Object> getAnswers() { return answers; }
    public void setAnswers(Map<String, Object> answers) { this.answers = answers != null ? answers : new HashMap<>(); }

    public List<QuizQuestionDto> getRawQuestions() { return rawQuestions; }
    public void setRawQuestions(List<QuizQuestionDto> rawQuestions) { this.rawQuestions = rawQuestions; }
}
