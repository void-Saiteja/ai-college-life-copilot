package com.collegecopilot.budget;

import com.collegecopilot.budget.dto.MealIdeaDto;
import com.collegecopilot.budget.dto.MealPlannerDataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiMealPlannerService {

    private static final Logger log = LoggerFactory.getLogger(GeminiMealPlannerService.class);

    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final List<String> GEMINI_MODELS = List.of(
            "gemini-1.5-flash",
            "gemini-2.0-flash",
            "gemini-2.5-flash"
    );

    private static final List<String> DANGEROUS_PATTERNS = List.of(
            "ignore previous instructions",
            "ignore all instructions",
            "ignore all rules",
            "reveal system prompt",
            "reveal the system prompt",
            "show system prompt",
            "reveal prompt",
            "reveal password",
            "reveal secret",
            "reveal api key",
            "show api key",
            "reveal another student",
            "reveal other student",
            "reveal notes of",
            "bypass security",
            "jailbreak"
    );

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public GeminiMealPlannerService() {
        this("");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiMealPlannerService(@Value("${GEMINI_API_KEY:${AI_API_KEY:}}") String apiKey) {
        this(apiKey, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    public GeminiMealPlannerService(String apiKey, HttpClient httpClient) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.httpClient = httpClient;
    }

    public boolean detectPromptInjection(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase();
        return DANGEROUS_PATTERNS.stream().anyMatch(lower::contains);
    }

    public MealPlannerDataDto generateMealPlan(BigDecimal budgetRemaining, List<String> ingredients) {
        double budgetVal = budgetRemaining != null ? budgetRemaining.doubleValue() : 0.0;
        List<String> sanitizedIngredients = sanitizeIngredients(ingredients);

        if (apiKey.isEmpty() || "mock_key".equalsIgnoreCase(apiKey) || "your_gemini_api_key_here".equalsIgnoreCase(apiKey)) {
            log.info("Gemini API key is not configured or mock; using intelligent local meal recommendations");
            return generateLocalFallbackMealPlan(budgetVal, sanitizedIngredients);
        }

        String prompt = buildPrompt(budgetVal, sanitizedIngredients);

        Exception lastError = null;
        for (String model : GEMINI_MODELS) {
            try {
                String payload = String.format("""
                        {
                          "contents": [
                            {
                              "role": "user",
                              "parts": [
                                { "text": %s }
                              ]
                            }
                          ],
                          "generationConfig": {
                            "temperature": 0.3,
                            "topK": 40,
                            "topP": 0.95,
                            "maxOutputTokens": 1024,
                            "responseMimeType": "application/json"
                          }
                        }
                        """, escapeJsonString(prompt));

                String endpoint = String.format(
                        "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                        model, apiKey
                );

                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(8))
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String extracted = extractTextFromGeminiResponse(response.body());
                    if (extracted != null && !extracted.isBlank()) {
                        MealPlannerDataDto parsed = parseStructuredJson(extracted);
                        if (parsed != null && parsed.getSuggestedMeals() != null && !parsed.getSuggestedMeals().isEmpty()) {
                            return parsed;
                        }
                    }
                }
            } catch (Exception e) {
                lastError = e;
                log.debug("Gemini model {} failed during meal plan generation: {}", model, e.getMessage());
            }
        }

        log.warn("Gemini AI API call failed or unparseable across models, falling back to local meal plan: {}",
                lastError != null ? lastError.getMessage() : "empty reply");
        return generateLocalFallbackMealPlan(budgetVal, sanitizedIngredients);
    }

    public MealPlannerDataDto generateLocalFallbackMealPlan(double budgetRemaining, List<String> ingredients) {
        String budgetAdvice = budgetRemaining < 100.0
                ? "⚠️ Budget tight this month! Stick to meal prep and campus dining events."
                : "👍 Healthy budget margin! Remember to put aside 15% into savings.";

        List<MealIdeaDto> meals = new ArrayList<>();

        if (ingredients != null && !ingredients.isEmpty()) {
            String primaryIng = ingredients.get(0);
            meals.add(new MealIdeaDto(
                    "Pantry Special: " + primaryIng + " Stir-Fry",
                    "$2.20",
                    "12 mins",
                    List.of(primaryIng, "Soy Sauce", "Garlic", "Rice"),
                    "Sauté " + primaryIng + " with garlic and soy sauce, serve over warm steamed rice."
            ));
        }

        meals.add(new MealIdeaDto(
                "Quick College Ramen Upgrade",
                "$2.50",
                "10 mins",
                List.of("Instant Ramen", "Egg", "Green Onions", "Spinach"),
                "Boil ramen, crack egg into broth during last 2 mins, stir in fresh spinach."
        ));

        meals.add(new MealIdeaDto(
                "Loaded High-Protein Burrito Bowl",
                "$4.20",
                "15 mins",
                List.of("Rice", "Black Beans", "Canned Chicken/Tofu", "Salsa", "Cheese"),
                "Heat rice and beans, top with salsa, cheese, and protein choice."
        ));

        meals.add(new MealIdeaDto(
                "5-Minute Peanut Butter Banana Oats",
                "$1.80",
                "5 mins",
                List.of("Rolled Oats", "Milk/Water", "Peanut Butter", "Banana", "Honey"),
                "Microwave oats with milk for 90s, swirl in peanut butter, top with sliced banana."
        ));

        // Return up to 3 or 4 suggested meals
        List<MealIdeaDto> resultMeals = meals.size() > 3 && ingredients != null && !ingredients.isEmpty()
                ? meals.subList(0, 3)
                : meals;

        return new MealPlannerDataDto(budgetAdvice, resultMeals);
    }

    private List<String> sanitizeIngredients(List<String> ingredients) {
        if (ingredients == null) return List.of();
        return ingredients.stream()
                .filter(i -> i != null && !i.isBlank())
                .map(i -> i.trim().length() > 50 ? i.trim().substring(0, 50) : i.trim())
                .limit(20)
                .toList();
    }

    private String buildPrompt(double budgetRemaining, List<String> ingredients) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert student budget and meal planning assistant for college students.\n");
        sb.append(String.format("The student has a remaining monthly budget of $%.2f.\n", budgetRemaining));
        if (ingredients != null && !ingredients.isEmpty()) {
            sb.append("Available ingredients in student's pantry: ").append(String.join(", ", ingredients)).append("\n");
        }
        sb.append("""
                Suggest 3 inexpensive, nutritious, fast meals tailored to a college student with this budget.
                Output ONLY a valid JSON object with the following exact schema:
                {
                  "budgetAdvice": "Brief 1-sentence budget tip based on their remaining budget",
                  "suggestedMeals": [
                    {
                      "name": "Meal Name",
                      "cost": "$X.XX",
                      "time": "XX mins",
                      "ingredients": ["item1", "item2"],
                      "instructions": "Simple 1-2 sentence recipe"
                    }
                  ]
                }
                Do not include markdown fences or any other text.
                """);
        return sb.toString();
    }

    private MealPlannerDataDto parseStructuredJson(String jsonText) {
        try {
            String clean = jsonText.trim();
            if (clean.startsWith("```json")) {
                clean = clean.substring(7);
            } else if (clean.startsWith("```")) {
                clean = clean.substring(3);
            }
            if (clean.endsWith("```")) {
                clean = clean.substring(0, clean.length() - 3);
            }
            clean = clean.trim();

            return objectMapper.readValue(clean, MealPlannerDataDto.class);
        } catch (Exception e) {
            log.debug("Failed to parse meal plan JSON: {}", e.getMessage());
            return null;
        }
    }

    private static String extractTextFromGeminiResponse(String responseBody) {
        if (responseBody == null) return null;
        Matcher matcher = TEXT_PATTERN.matcher(responseBody);
        if (matcher.find()) {
            String raw = matcher.group(1);
            return raw.replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .trim();
        }
        return null;
    }

    private static String escapeJsonString(String input) {
        if (input == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}
