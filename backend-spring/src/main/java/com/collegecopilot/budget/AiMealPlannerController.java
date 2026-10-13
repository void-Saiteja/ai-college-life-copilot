package com.collegecopilot.budget;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.budget.dto.MealPlannerRequest;
import com.collegecopilot.budget.dto.MealPlannerResponse;
import com.collegecopilot.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class AiMealPlannerController {

    private final BudgetService budgetService;

    @Autowired
    public AiMealPlannerController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        return principal;
    }

    // POST /api/ai/meal-planner
    @PostMapping({"/api/ai/meal-planner", "/api/budget/meal-planner"})
    public ResponseEntity<MealPlannerResponse> planMeals(@RequestBody MealPlannerRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(budgetService.planMeals(request, principal));
    }
}
