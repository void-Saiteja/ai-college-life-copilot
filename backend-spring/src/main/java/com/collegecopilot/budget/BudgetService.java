package com.collegecopilot.budget;

import com.collegecopilot.auth.AuthException;
import com.collegecopilot.budget.dto.*;
import com.collegecopilot.security.UserPrincipal;
import com.collegecopilot.student.Student;
import com.collegecopilot.student.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final StudentRepository studentRepository;
    private final GeminiMealPlannerService mealPlannerService;

    @Autowired
    public BudgetService(BudgetRepository budgetRepository,
                         ExpenseRepository expenseRepository,
                         StudentRepository studentRepository,
                         GeminiMealPlannerService mealPlannerService) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.studentRepository = studentRepository;
        this.mealPlannerService = mealPlannerService;
    }

    public String resolveStudentId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }
        String studentKey = principal.getStudentId() != null ? principal.getStudentId() : principal.getId();
        if (studentKey == null || studentKey.isBlank()) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "Unauthorized: Student authentication required");
        }

        Optional<Student> direct = studentRepository.findById(studentKey);
        if (direct.isPresent()) {
            return direct.get().getId();
        }

        Optional<Student> byUser = studentRepository.findByUserId(studentKey);
        return byUser.map(Student::getId).orElse(studentKey);
    }

    @Transactional
    public Budget getOrCreateBudget(String studentId) {
        return budgetRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    try {
                        Budget newBudget = new Budget(
                                "bgt-" + UUID.randomUUID().toString().substring(0, 8),
                                studentId,
                                BigDecimal.valueOf(1200.00),
                                "$"
                        );
                        return budgetRepository.save(newBudget);
                    } catch (Exception e) {
                        return budgetRepository.findByStudentId(studentId)
                                .orElse(new Budget("bgt-fallback", studentId, BigDecimal.valueOf(1200.00), "$"));
                    }
                });
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudget(UserPrincipal principal) {
        String studentId = resolveStudentId(principal);
        Budget budget = getOrCreateBudget(studentId);
        List<Expense> expenses = expenseRepository.findByStudentIdOrderByExpenseDateDescCreatedAtDesc(studentId);

        BudgetDataDto data = buildBudgetDataDto(budget, expenses);
        return new BudgetResponse(true, data);
    }

    @Transactional
    public ExpenseSingleResponse addExpense(ExpenseCreateRequest request, UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required (1-150 characters)");
        }

        String title = request.getTitle();
        if (title == null || title.isBlank() || title.length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required (1-150 characters)");
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(BigDecimal.valueOf(100000)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be a positive number up to $100,000");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);

        String category = request.getCategory();
        if (category != null && category.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category must be a string up to 50 characters");
        }
        category = (category != null && !category.isBlank()) ? category.trim() : "General";

        LocalDate expenseDate = parseExpenseDate(request.getDate());

        String id = "exp-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4);

        Expense expense = new Expense(
                id,
                studentId,
                title.trim(),
                amount,
                category,
                expenseDate
        );

        Expense saved = expenseRepository.save(expense);
        return new ExpenseSingleResponse(true, toExpenseDto(saved));
    }

    @Transactional
    public BudgetResponse updateBudgetTarget(BudgetTargetUpdateRequest request, UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        if (request == null || request.getMonthlyTarget() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid monthlyTarget must be a positive number up to $1,000,000");
        }

        BigDecimal monthlyTarget = request.getMonthlyTarget();
        if (monthlyTarget.compareTo(BigDecimal.ZERO) <= 0 || monthlyTarget.compareTo(BigDecimal.valueOf(1000000)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid monthlyTarget must be a positive number up to $1,000,000");
        }
        monthlyTarget = monthlyTarget.setScale(2, RoundingMode.HALF_UP);

        Budget budget = getOrCreateBudget(studentId);
        budget.setMonthlyTarget(monthlyTarget);
        Budget savedBudget = budgetRepository.save(budget);

        List<Expense> expenses = expenseRepository.findByStudentIdOrderByExpenseDateDescCreatedAtDesc(studentId);
        BudgetDataDto data = buildBudgetDataDto(savedBudget, expenses);

        return new BudgetResponse(true, data);
    }

    @Transactional
    public ExpenseDeleteResponse deleteExpense(String id, UserPrincipal principal) {
        String studentId = resolveStudentId(principal);

        Expense item = expenseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found in your budget"));

        boolean isOwner = studentId.equals(item.getStudentId())
                || (item.getStudentId() == null && "std-1".equals(studentId));

        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this expense");
        }

        expenseRepository.delete(item);
        return new ExpenseDeleteResponse(true, "Expense deleted successfully");
    }

    public MealPlannerResponse planMeals(MealPlannerRequest request, UserPrincipal principal) {
        resolveStudentId(principal);

        if (request == null || request.getBudgetRemaining() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid non-negative budgetRemaining amount is required (0 - 1,000,000)");
        }

        BigDecimal budget = request.getBudgetRemaining();
        if (budget.compareTo(BigDecimal.ZERO) < 0 || budget.compareTo(BigDecimal.valueOf(1000000)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid non-negative budgetRemaining amount is required (0 - 1,000,000)");
        }

        List<String> ingredients = request.getIngredients() != null ? request.getIngredients() : List.of();
        boolean hasInjection = ingredients.stream()
                .filter(Objects::nonNull)
                .anyMatch(mealPlannerService::detectPromptInjection);

        if (hasInjection) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ingredient input detected");
        }

        MealPlannerDataDto data = mealPlannerService.generateMealPlan(budget, ingredients);
        return new MealPlannerResponse(true, data);
    }

    private LocalDate parseExpenseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            String trimmed = dateStr.trim();
            if (trimmed.contains("T")) {
                trimmed = trimmed.substring(0, trimmed.indexOf("T"));
            }
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private BudgetDataDto buildBudgetDataDto(Budget budget, List<Expense> expenses) {
        BigDecimal totalSpent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();

        List<ExpenseDto> dtos = new ArrayList<>();
        for (Expense exp : expenses) {
            BigDecimal amt = exp.getAmount() != null ? exp.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            totalSpent = totalSpent.add(amt);

            String cat = (exp.getCategory() != null && !exp.getCategory().isBlank()) ? exp.getCategory() : "General";
            breakdown.put(cat, breakdown.getOrDefault(cat, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)).add(amt));

            dtos.add(toExpenseDto(exp));
        }

        BigDecimal target = budget.getMonthlyTarget() != null
                ? budget.getMonthlyTarget().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(1200.00).setScale(2, RoundingMode.HALF_UP);

        BigDecimal remaining = target.subtract(totalSpent).setScale(2, RoundingMode.HALF_UP);

        return new BudgetDataDto(
                budget.getId(),
                budget.getStudentId(),
                target,
                budget.getCurrency() != null ? budget.getCurrency() : "$",
                totalSpent,
                remaining,
                breakdown,
                dtos
        );
    }

    public ExpenseDto toExpenseDto(Expense exp) {
        if (exp == null) return null;
        String dateStr = exp.getExpenseDate() != null ? exp.getExpenseDate().toString() : LocalDate.now().toString();
        String createdStr = exp.getCreatedAt() != null ? exp.getCreatedAt().toString() : null;
        String updatedStr = exp.getUpdatedAt() != null ? exp.getUpdatedAt().toString() : null;

        return new ExpenseDto(
                exp.getId(),
                exp.getStudentId(),
                exp.getTitle(),
                exp.getAmount() != null ? exp.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO,
                exp.getCategory(),
                dateStr,
                createdStr,
                updatedStr
        );
    }
}
