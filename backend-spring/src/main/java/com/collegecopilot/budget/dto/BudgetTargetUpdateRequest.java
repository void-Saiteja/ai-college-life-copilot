package com.collegecopilot.budget.dto;

import java.math.BigDecimal;

public class BudgetTargetUpdateRequest {

    private BigDecimal monthlyTarget;

    public BudgetTargetUpdateRequest() {
    }

    public BudgetTargetUpdateRequest(BigDecimal monthlyTarget) {
        this.monthlyTarget = monthlyTarget;
    }

    public BigDecimal getMonthlyTarget() {
        return monthlyTarget;
    }

    public void setMonthlyTarget(BigDecimal monthlyTarget) {
        this.monthlyTarget = monthlyTarget;
    }
}
