package com.collegecopilot.budget.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class ExpenseDto {

    private String id;

    @JsonProperty("student_id")
    private String studentId;

    private String title;
    private BigDecimal amount;
    private String category;

    @JsonProperty("date")
    private String date;

    @JsonProperty("expense_date")
    private String expenseDate;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    public ExpenseDto() {
    }

    public ExpenseDto(String id, String studentId, String title, BigDecimal amount, String category, String date, String createdAt, String updatedAt) {
        this.id = id;
        this.studentId = studentId;
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.expenseDate = date;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
        this.expenseDate = date;
    }

    public String getExpenseDate() {
        return expenseDate != null ? expenseDate : date;
    }

    public void setExpenseDate(String expenseDate) {
        this.expenseDate = expenseDate;
        if (this.date == null) {
            this.date = expenseDate;
        }
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
