package com.collegecopilot.budget;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.budget.dto.*;
import com.collegecopilot.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/budget")
@CrossOrigin(origins = "*")
public class BudgetController {

    private final BudgetService budgetService;

    @Autowired
    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    private UserPrincipal getAuthenticatedPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        return principal;
    }

    // GET /api/budget
    @GetMapping
    public ResponseEntity<BudgetResponse> getBudget() {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(budgetService.getBudget(principal));
    }

    // POST /api/budget/expense
    @PostMapping("/expense")
    public ResponseEntity<ExpenseSingleResponse> addExpense(@RequestBody ExpenseCreateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        ExpenseSingleResponse response = budgetService.addExpense(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // PUT /api/budget/target
    @PutMapping("/target")
    public ResponseEntity<BudgetResponse> updateBudgetTarget(@RequestBody BudgetTargetUpdateRequest request) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(budgetService.updateBudgetTarget(request, principal));
    }

    // DELETE /api/budget/expense/{id}
    @DeleteMapping("/expense/{id}")
    public ResponseEntity<ExpenseDeleteResponse> deleteExpense(@PathVariable("id") String id) {
        UserPrincipal principal = getAuthenticatedPrincipal();
        return ResponseEntity.ok(budgetService.deleteExpense(id, principal));
    }
}
