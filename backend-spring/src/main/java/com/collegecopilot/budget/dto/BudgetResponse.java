package com.collegecopilot.budget.dto;

public class BudgetResponse {

    private boolean success;
    private BudgetDataDto data;

    public BudgetResponse() {
    }

    public BudgetResponse(boolean success, BudgetDataDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public BudgetDataDto getData() {
        return data;
    }

    public void setData(BudgetDataDto data) {
        this.data = data;
    }
}
