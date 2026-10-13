package com.collegecopilot.note.dto;

import java.util.ArrayList;
import java.util.List;

public class NoteSummaryDataDto {

    private String title;
    private String course;
    private Integer wordCount;
    private Integer readingTimeMinutes;
    private String summary;
    private List<String> keyTakeaways = new ArrayList<>();
    private List<FlashcardDto> flashcards = new ArrayList<>();
    private List<QuizItemDto> quiz = new ArrayList<>();

    public NoteSummaryDataDto() {
    }

    public NoteSummaryDataDto(String title, String course, Integer wordCount, Integer readingTimeMinutes,
                              String summary, List<String> keyTakeaways, List<FlashcardDto> flashcards,
                              List<QuizItemDto> quiz) {
        this.title = title;
        this.course = course;
        this.wordCount = wordCount;
        this.readingTimeMinutes = readingTimeMinutes;
        this.summary = summary;
        this.keyTakeaways = keyTakeaways != null ? keyTakeaways : new ArrayList<>();
        this.flashcards = flashcards != null ? flashcards : new ArrayList<>();
        this.quiz = quiz != null ? quiz : new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public Integer getReadingTimeMinutes() {
        return readingTimeMinutes;
    }

    public void setReadingTimeMinutes(Integer readingTimeMinutes) {
        this.readingTimeMinutes = readingTimeMinutes;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getKeyTakeaways() {
        return keyTakeaways;
    }

    public void setKeyTakeaways(List<String> keyTakeaways) {
        this.keyTakeaways = keyTakeaways != null ? keyTakeaways : new ArrayList<>();
    }

    public List<FlashcardDto> getFlashcards() {
        return flashcards;
    }

    public void setFlashcards(List<FlashcardDto> flashcards) {
        this.flashcards = flashcards != null ? flashcards : new ArrayList<>();
    }

    public List<QuizItemDto> getQuiz() {
        return quiz;
    }

    public void setQuiz(List<QuizItemDto> quiz) {
        this.quiz = quiz != null ? quiz : new ArrayList<>();
    }
}
