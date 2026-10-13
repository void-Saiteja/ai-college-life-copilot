package com.collegecopilot.budget.dto;

public class MealPlannerResponse {

    private boolean success;
    private MealPlannerDataDto data;

    public MealPlannerResponse() {
    }

    public MealPlannerResponse(boolean success, MealPlannerDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public MealPlannerDataDto getData() {
        return data;
    }

    public void setData(MealPlannerDataDto data) {
        this.data = data;
    }
}
