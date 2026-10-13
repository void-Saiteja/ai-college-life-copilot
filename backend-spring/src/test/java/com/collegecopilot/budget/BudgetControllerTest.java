package com.collegecopilot.budget;

import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.budget.dto.*;
import com.collegecopilot.security.JwtAuthenticationFilter;
import com.collegecopilot.security.JwtTokenProvider;
import com.collegecopilot.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BudgetControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private BudgetController budgetController;

    private AiMealPlannerController aiMealPlannerController;

    private JwtTokenProvider tokenProvider;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        tokenA = tokenProvider.generateToken("u-1", "Alex Mercer", "alex@student.edu", "STUDENT", "std-1", null);
        tokenB = tokenProvider.generateToken("u-2", "Bob Smith", "bob@student.edu", "STUDENT", "std-2", null);

        aiMealPlannerController = new AiMealPlannerController(budgetService);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(budgetController, aiMealPlannerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/budget requires JWT token and returns 401 if missing")
    void testGetBudgetUnauthorized() throws Exception {
        mockMvc.perform(get("/api/budget"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/budget returns 200 OK with budget data for authenticated student")
    void testGetBudgetSuccess() throws Exception {
        BudgetDataDto data = new BudgetDataDto(
                "bgt-1",
                "std-1",
                BigDecimal.valueOf(1200.00),
                "$",
                BigDecimal.valueOf(250.00),
                BigDecimal.valueOf(950.00),
                Map.of("Food", BigDecimal.valueOf(250.00)),
                List.of(new ExpenseDto("exp-1", "std-1", "Groceries", BigDecimal.valueOf(250.00), "Food", "2026-10-10", null, null))
        );

        when(budgetService.getBudget(any(UserPrincipal.class))).thenReturn(new BudgetResponse(true, data));

        mockMvc.perform(get("/api/budget")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyTarget").value(1200.00))
                .andExpect(jsonPath("$.data.totalSpent").value(250.00))
                .andExpect(jsonPath("$.data.remaining").value(950.00))
                .andExpect(jsonPath("$.data.expenses[0].title").value("Groceries"))
                .andExpect(jsonPath("$.data.expenses[0].date").value("2026-10-10"));
    }

    @Test
    @DisplayName("POST /api/budget/expense creates expense and returns 201 Created")
    void testAddExpenseSuccess() throws Exception {
        ExpenseDto dto = new ExpenseDto("exp-123", "std-1", "Textbooks", BigDecimal.valueOf(75.50), "Academic", "2026-10-10", null, null);
        when(budgetService.addExpense(any(ExpenseCreateRequest.class), any(UserPrincipal.class)))
                .thenReturn(new ExpenseSingleResponse(true, dto));

        String json = """
                {
                  "title": "Textbooks",
                  "amount": 75.50,
                  "category": "Academic"
                }
                """;

        mockMvc.perform(post("/api/budget/expense")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("exp-123"))
                .andExpect(jsonPath("$.data.amount").value(75.50))
                .andExpect(jsonPath("$.data.category").value("Academic"));
    }

    @Test
    @DisplayName("POST /api/budget/expense returns 400 when title or amount is invalid")
    void testAddExpenseBadRequest() throws Exception {
        when(budgetService.addExpense(any(ExpenseCreateRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required (1-150 characters)"));

        String json = """
                {
                  "title": "",
                  "amount": 10.0
                }
                """;

        mockMvc.perform(post("/api/budget/expense")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Title is required (1-150 characters)"));
    }

    @Test
    @DisplayName("PUT /api/budget/target updates target and returns 200 OK")
    void testUpdateBudgetTargetSuccess() throws Exception {
        BudgetDataDto data = new BudgetDataDto(
                "bgt-1", "std-1", BigDecimal.valueOf(1500.00), "$",
                BigDecimal.ZERO, BigDecimal.valueOf(1500.00), Map.of(), List.of()
        );
        when(budgetService.updateBudgetTarget(any(BudgetTargetUpdateRequest.class), any(UserPrincipal.class)))
                .thenReturn(new BudgetResponse(true, data));

        String json = """
                {
                  "monthlyTarget": 1500.00
                }
                """;

        mockMvc.perform(put("/api/budget/target")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.monthlyTarget").value(1500.00));
    }

    @Test
    @DisplayName("PUT /api/budget/target returns 400 on invalid target value")
    void testUpdateBudgetTargetBadRequest() throws Exception {
        when(budgetService.updateBudgetTarget(any(BudgetTargetUpdateRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid monthlyTarget must be a positive number up to $1,000,000"));

        String json = """
                {
                  "monthlyTarget": -50.00
                }
                """;

        mockMvc.perform(put("/api/budget/target")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Valid monthlyTarget must be a positive number up to $1,000,000"));
    }

    @Test
    @DisplayName("DELETE /api/budget/expense/{id} returns 200 OK on successful deletion")
    void testDeleteExpenseSuccess() throws Exception {
        when(budgetService.deleteExpense(eq("exp-1"), any(UserPrincipal.class)))
                .thenReturn(new ExpenseDeleteResponse(true, "Expense deleted successfully"));

        mockMvc.perform(delete("/api/budget/expense/exp-1")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Expense deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/budget/expense/{id} returns 404 when expense does not exist")
    void testDeleteExpenseNotFound() throws Exception {
        when(budgetService.deleteExpense(eq("exp-missing"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found in your budget"));

        mockMvc.perform(delete("/api/budget/expense/exp-missing")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Expense not found in your budget"));
    }

    @Test
    @DisplayName("DELETE /api/budget/expense/{id} returns 403 when trying to delete another student's expense")
    void testDeleteExpenseForbiddenCrossStudent() throws Exception {
        when(budgetService.deleteExpense(eq("exp-other"), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: You do not own this expense"));

        mockMvc.perform(delete("/api/budget/expense/exp-other")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Access denied: You do not own this expense"));
    }

    @Test
    @DisplayName("POST /api/ai/meal-planner returns 200 OK with meal suggestions")
    void testAiMealPlannerSuccess() throws Exception {
        MealPlannerDataDto data = new MealPlannerDataDto(
                "👍 Healthy budget margin! Remember to put aside 15% into savings.",
                List.of(new MealIdeaDto(
                        "Quick College Ramen Upgrade",
                        "$2.50",
                        "10 mins",
                        List.of("Instant Ramen", "Egg", "Spinach"),
                        "Boil ramen, crack egg into broth."
                ))
        );

        when(budgetService.planMeals(any(MealPlannerRequest.class), any(UserPrincipal.class)))
                .thenReturn(new MealPlannerResponse(true, data));

        String json = """
                {
                  "budgetRemaining": 750.00,
                  "ingredients": ["Ramen", "Egg"]
                }
                """;

        mockMvc.perform(post("/api/ai/meal-planner")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.budgetAdvice").value("👍 Healthy budget margin! Remember to put aside 15% into savings."))
                .andExpect(jsonPath("$.data.suggestedMeals[0].name").value("Quick College Ramen Upgrade"))
                .andExpect(jsonPath("$.data.suggestedMeals[0].cost").value("$2.50"));
    }

    @Test
    @DisplayName("POST /api/ai/meal-planner returns 400 when budget amount is negative or invalid")
    void testAiMealPlannerBadRequest() throws Exception {
        when(budgetService.planMeals(any(MealPlannerRequest.class), any(UserPrincipal.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid non-negative budgetRemaining amount is required (0 - 1,000,000)"));

        String json = """
                {
                  "budgetRemaining": -50.00
                }
                """;

        mockMvc.perform(post("/api/ai/meal-planner")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Valid non-negative budgetRemaining amount is required (0 - 1,000,000)"));
    }
}
