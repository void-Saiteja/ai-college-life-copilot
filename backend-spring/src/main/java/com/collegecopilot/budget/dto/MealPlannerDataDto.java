package com.collegecopilot.budget.dto;

import java.util.ArrayList;
import java.util.List;

public class MealPlannerDataDto {

    private String budgetAdvice;
    private List<MealIdeaDto> suggestedMeals = new ArrayList<>();

    public MealPlannerDataDto() {
    }

    public MealPlannerDataDto(String budgetAdvice, List<MealIdeaDto> suggestedMeals) {
        this.budgetAdvice = budgetAdvice;
        this.suggestedMeals = suggestedMeals != null ? suggestedMeals : new ArrayList<>();
    }

    public String getBudgetAdvice() {
        return budgetAdvice;
    }

    public void setBudgetAdvice(String budgetAdvice) {
        this.budgetAdvice = budgetAdvice;
    }

    public List<MealIdeaDto> getSuggestedMeals() {
        return suggestedMeals;
    }

    public void setSuggestedMeals(List<MealIdeaDto> suggestedMeals) {
        this.suggestedMeals = suggestedMeals;
    }
}
