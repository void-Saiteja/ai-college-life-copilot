package com.collegecopilot.budget.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MealPlannerRequest {

    private BigDecimal budgetRemaining;
    private List<String> ingredients = new ArrayList<>();

    public MealPlannerRequest() {
    }

    public MealPlannerRequest(BigDecimal budgetRemaining, List<String> ingredients) {
        this.budgetRemaining = budgetRemaining;
        this.ingredients = ingredients != null ? ingredients : new ArrayList<>();
    }

    public BigDecimal getBudgetRemaining() {
        return budgetRemaining;
    }

    public void setBudgetRemaining(BigDecimal budgetRemaining) {
        this.budgetRemaining = budgetRemaining;
    }

    public List<String> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }
}
