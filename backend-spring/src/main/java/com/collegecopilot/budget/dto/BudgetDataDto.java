package com.collegecopilot.budget.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BudgetDataDto {

    private String id;

    @JsonProperty("student_id")
    private String studentId;

    private BigDecimal monthlyTarget;
    private String currency;
    private BigDecimal totalSpent;
    private BigDecimal remaining;
    private Map<String, BigDecimal> categoryBreakdown = new LinkedHashMap<>();
    private List<ExpenseDto> expenses = new ArrayList<>();

    public BudgetDataDto() {
    }

    public BudgetDataDto(String id, String studentId, BigDecimal monthlyTarget, String currency,
                         BigDecimal totalSpent, BigDecimal remaining,
                         Map<String, BigDecimal> categoryBreakdown, List<ExpenseDto> expenses) {
        this.id = id;
        this.studentId = studentId;
        this.monthlyTarget = monthlyTarget;
        this.currency = currency;
        this.totalSpent = totalSpent;
        this.remaining = remaining;
        this.categoryBreakdown = categoryBreakdown != null ? categoryBreakdown : new LinkedHashMap<>();
        this.expenses = expenses != null ? expenses : new ArrayList<>();
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

    public BigDecimal getMonthlyTarget() {
        return monthlyTarget;
    }

    public void setMonthlyTarget(BigDecimal monthlyTarget) {
        this.monthlyTarget = monthlyTarget;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public BigDecimal getRemaining() {
        return remaining;
    }

    public void setRemaining(BigDecimal remaining) {
        this.remaining = remaining;
    }

    public Map<String, BigDecimal> getCategoryBreakdown() {
        return categoryBreakdown;
    }

    public void setCategoryBreakdown(Map<String, BigDecimal> categoryBreakdown) {
        this.categoryBreakdown = categoryBreakdown;
    }

    public List<ExpenseDto> getExpenses() {
        return expenses;
    }

    public void setExpenses(List<ExpenseDto> expenses) {
        this.expenses = expenses;
    }
}
