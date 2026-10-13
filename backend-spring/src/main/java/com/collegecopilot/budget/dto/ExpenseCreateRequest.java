package com.collegecopilot.budget.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class ExpenseCreateRequest {

    private String title;
    private BigDecimal amount;
    private String category;

    @JsonProperty("date")
    private String date;

    @JsonProperty("expense_date")
    private String expenseDate;

    public ExpenseCreateRequest() {
    }

    public ExpenseCreateRequest(String title, BigDecimal amount, String category, String date) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.expenseDate = date;
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
        return date != null ? date : expenseDate;
    }

    public void setDate(String date) {
        this.date = date;
        if (this.expenseDate == null) {
            this.expenseDate = date;
        }
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
}
