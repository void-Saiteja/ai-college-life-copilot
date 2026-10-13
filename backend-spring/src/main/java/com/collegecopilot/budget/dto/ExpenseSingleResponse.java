package com.collegecopilot.budget.dto;

public class ExpenseSingleResponse {

    private boolean success;
    private ExpenseDto data;

    public ExpenseSingleResponse() {
    }

    public ExpenseSingleResponse(boolean success, ExpenseDto data) {
        this.success = success;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ExpenseDto getData() {
        return data;
    }

    public void setData(ExpenseDto data) {
        this.data = data;
    }
}
