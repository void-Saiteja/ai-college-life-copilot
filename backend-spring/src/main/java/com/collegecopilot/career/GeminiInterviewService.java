package com.collegecopilot.career;

import com.collegecopilot.career.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeminiInterviewService {

    private static final Logger log = LoggerFactory.getLogger(GeminiInterviewService.class);

    public static final List<String> VALID_CATEGORIES = List.of(
            "TECHNICAL", "BEHAVIORAL", "PROJECT", "ROLE_SPECIFIC"
    );

    public static final List<String> VALID_DIFFICULTIES = List.of(
            "EASY", "MEDIUM", "HARD"
    );

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

    public record FallbackQuestion(String skill, String question, String tips) {}

    private static final Map<String, Map<String, List<FallbackQuestion>>> QUESTION_BANK = new LinkedHashMap<>();

    static {
        Map<String, List<FallbackQuestion>> tech = new LinkedHashMap<>();
        tech.put("EASY", List.of(
                new FallbackQuestion("JavaScript", "Explain the difference between let, const, and var in modern JavaScript.", "Discuss scoping rules and hoisting."),
                new FallbackQuestion("SQL", "What is the difference between WHERE and HAVING clauses in SQL queries?", "Mention aggregation functions."),
                new FallbackQuestion("React", "What are React hooks, and how does the useState hook manage component state?", "Explain functional components and state immutability."),
                new FallbackQuestion("Docker", "What is the primary difference between a Docker image and a Docker container?", "Images are immutable templates; containers are running instances."),
                new FallbackQuestion("Git", "Explain the purpose of git branch and how you resolve a merge conflict.", "Mention git checkout / switch and merge markers.")
        ));
        tech.put("MEDIUM", List.of(
                new FallbackQuestion("JavaScript", "How does the JavaScript Event Loop handle asynchronous callbacks and promises (Microtasks vs Macrotasks)?", "Explain Call Stack, Web APIs, Microtask Queue, and Callback Queue."),
                new FallbackQuestion("Node.js", "How does Node.js handle concurrent I/O operations despite having a single-threaded event loop?", "Discuss libuv, worker threads, and non-blocking I/O."),
                new FallbackQuestion("SQL", "Explain database indexing: how do B-Trees optimize query lookups, and when might an index degrade performance?", "Discuss search time complexity O(log n) vs insert/update overhead."),
                new FallbackQuestion("React", "Explain the React virtual DOM reconciliation algorithm and how keys help optimize list rendering.", "Explain tree diffing and identity persistence."),
                new FallbackQuestion("Docker", "How would you write a multi-stage Dockerfile to minimize production image size for a Node.js or React application?", "Discuss build stage vs runtime stage."),
                new FallbackQuestion("REST APIs", "What are idempotent HTTP methods, and why are PUT and DELETE considered idempotent while POST is not?", "Explain the definition of idempotence and side effects.")
        ));
        tech.put("HARD", List.of(
                new FallbackQuestion("Node.js", "Design a high-throughput rate limiter middleware in Express/Node.js using Redis sliding windows or token buckets.", "Discuss race conditions, memory bounds, and atomic operations."),
                new FallbackQuestion("SQL", "How do database transaction isolation levels (Read Committed, Repeatable Read, Serializable) prevent dirty reads, non-repeatable reads, and phantom reads?", "Discuss MVCC and row/table locks."),
                new FallbackQuestion("Docker", "Explain container networking modes (bridge, host, overlay) and how Docker manages container-to-container communication with DNS and iptables.", "Discuss virtual ethernet pairs, network namespaces, and port mapping.")
        ));
        QUESTION_BANK.put("TECHNICAL", tech);

        Map<String, List<FallbackQuestion>> beh = new LinkedHashMap<>();
        beh.put("EASY", List.of(
                new FallbackQuestion("Teamwork", "Tell me about a time you collaborated with peers on an academic software project. How did you divide responsibilities?", "Use the STAR format: Situation, Task, Action, Result."),
                new FallbackQuestion("Communication", "How do you approach explaining a technical concept to a non-technical teammate or stakeholder?", "Mention analogies, diagrams, and active listening.")
        ));
        beh.put("MEDIUM", List.of(
                new FallbackQuestion("Conflict Resolution", "Describe a situation where you disagreed with a team member about a technical architecture decision. How did you resolve it constructively?", "Focus on evaluating trade-offs, objective data, and consensus."),
                new FallbackQuestion("Time Management", "How do you prioritize multiple competing academic deadlines when an assignment and an exam overlap?", "Mention impact prioritization, time-boxing, and proactive communication.")
        ));
        beh.put("HARD", List.of(
                new FallbackQuestion("Leadership", "Describe a scenario where a critical project was falling significantly behind schedule. What concrete steps did you take to triage and deliver successfully?", "Discuss scope reduction, prioritization, and team coordination under pressure.")
        ));
        QUESTION_BANK.put("BEHAVIORAL", beh);

        Map<String, List<FallbackQuestion>> proj = new LinkedHashMap<>();
        proj.put("EASY", List.of(
                new FallbackQuestion("Full Stack", "Walk me through the architecture of a full-stack web project you built. What technologies did you choose and why?", "Highlight frontend, backend API, and database choices.")
        ));
        proj.put("MEDIUM", List.of(
                new FallbackQuestion("Database Design", "Describe a project where you designed the database schema. How did you decide between relational (SQL) and document (NoSQL) storage?", "Discuss schema consistency, relationship complexity, and query patterns.")
        ));
        proj.put("HARD", List.of(
                new FallbackQuestion("System Scaling", "If your main college project suddenly received 10,000 concurrent requests per second, what bottlenecks would arise first and how would you resolve them?", "Discuss caching, connection pooling, horizontal scaling, and CDN.")
        ));
        QUESTION_BANK.put("PROJECT", proj);

        Map<String, List<FallbackQuestion>> role = new LinkedHashMap<>();
        role.put("EASY", List.of(
                new FallbackQuestion("Web Development", "As a junior developer, what steps do you take when troubleshooting a bug reported in production?", "Mention error logs, reproducible test cases, and git bisect.")
        ));
        role.put("MEDIUM", List.of(
                new FallbackQuestion("Full Stack Developer", "As a Full Stack Developer, how do you enforce secure authentication and data authorization across both the frontend client and backend API?", "Mention JWTs, HttpOnly cookies, RBAC, and input sanitization.")
        ));
        role.put("HARD", List.of(
                new FallbackQuestion("Architecture", "For your target role, how do you balance rapid feature delivery with code quality, automated test coverage, and technical debt management?", "Discuss CI/CD, modular architecture, and automated testing.")
        ));
        QUESTION_BANK.put("ROLE_SPECIFIC", role);
    }

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public GeminiInterviewService() {
        this("");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiInterviewService(@Value("${GEMINI_API_KEY:${AI_API_KEY:}}") String apiKey) {
        this(apiKey, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    public GeminiInterviewService(String apiKey, HttpClient httpClient) {
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

    public record GeneratedQuestionResult(
            String question,
            String category,
            String difficulty,
            String targetSkill,
            String preparationTips,
            boolean isAiGenerated
    ) {}

    public GeneratedQuestionResult generateInterviewQuestion(
            String targetRole,
            String category,
            String difficulty,
            String skill,
            CareerProfile careerProfile,
            SkillGapAnalysisDto skillGapAnalysis) {

        String normCategory = (category != null && VALID_CATEGORIES.contains(category.trim().toUpperCase()))
                ? category.trim().toUpperCase()
                : "TECHNICAL";

        String normDifficulty = (difficulty != null && VALID_DIFFICULTIES.contains(difficulty.trim().toUpperCase()))
                ? difficulty.trim().toUpperCase()
                : "MEDIUM";

        String role = (targetRole != null && CareerService.ROLE_SKILL_CATALOG.containsKey(targetRole))
                ? targetRole
                : "Full Stack Developer";

        String targetSkill = skill;
        if (targetSkill == null || targetSkill.isBlank()) {
            if (skillGapAnalysis != null && skillGapAnalysis.getMissingList() != null && !skillGapAnalysis.getMissingList().isEmpty()) {
                targetSkill = skillGapAnalysis.getMissingList().get(0);
            } else if (skillGapAnalysis != null && skillGapAnalysis.getPartialList() != null && !skillGapAnalysis.getPartialList().isEmpty()) {
                targetSkill = skillGapAnalysis.getPartialList().get(0);
            } else {
                List<String> roleSkills = CareerService.ROLE_SKILL_CATALOG.getOrDefault(role,
                        CareerService.ROLE_SKILL_CATALOG.get("Full Stack Developer"));
                targetSkill = !roleSkills.isEmpty() ? roleSkills.get(0) : "Software Engineering";
            }
        }

        // Get deterministic fallback question from question bank
        Map<String, List<FallbackQuestion>> catMap = QUESTION_BANK.getOrDefault(normCategory, QUESTION_BANK.get("TECHNICAL"));
        List<FallbackQuestion> diffList = catMap.getOrDefault(normDifficulty, catMap.get("MEDIUM"));
        if (diffList == null || diffList.isEmpty()) {
            diffList = QUESTION_BANK.get("TECHNICAL").get("MEDIUM");
        }

        final String skillSearch = targetSkill;
        FallbackQuestion matched = diffList.stream()
                .filter(q -> q.skill().equalsIgnoreCase(skillSearch))
                .findFirst()
                .orElse(diffList.get(0));

        GeneratedQuestionResult fallbackResult = new GeneratedQuestionResult(
                matched.question(),
                normCategory,
                normDifficulty,
                targetSkill,
                matched.tips(),
                false
        );

        if (apiKey.isEmpty() || "mock_key".equalsIgnoreCase(apiKey) || "your_gemini_api_key_here".equalsIgnoreCase(apiKey)) {
            log.info("Gemini API key is unconfigured or mock; using deterministic interview question fallback");
            return fallbackResult;
        }

        if (detectPromptInjection(role) || detectPromptInjection(targetSkill)) {
            log.warn("Prompt injection detected in interview question request. Using deterministic fallback.");
            return fallbackResult;
        }

        try {
            String prompt = """
                    You are a Lead Software Engineering Interviewer conducting mock interviews for university students.
                    Generate ONE interview question tailored to the student's target role and skill gap.

                    SPECIFICATIONS:
                    - Target Role: %s
                    - Category: %s (must be strictly TECHNICAL, BEHAVIORAL, PROJECT, or ROLE_SPECIFIC)
                    - Difficulty: %s (must be strictly EASY, MEDIUM, or HARD)
                    - Target Skill/Focus: %s

                    STRICT ANTI-HALLUCINATION RULES:
                    - Do NOT assume personal details about the student's real-life jobs or identity.
                    - Frame the question professionally around computer science concepts, problem-solving, or engineering scenarios.

                    Return JSON in this exact format:
                    {
                      "question": "The question text here...",
                      "category": "%s",
                      "difficulty": "%s",
                      "target_skill": "%s",
                      "preparation_tips": "Guidance on how the student should structure their response."
                    }
                    """.formatted(role, normCategory, normDifficulty, targetSkill, normCategory, normDifficulty, targetSkill);

            for (String model : GEMINI_MODELS) {
                try {
                    String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
                    Map<String, Object> reqBody = Map.of(
                            "contents", List.of(
                                    Map.of("parts", List.of(Map.of("text", prompt)))
                            )
                    );
                    String jsonPayload = objectMapper.writeValueAsString(reqBody);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .header("Content-Type", "application/json")
                            .timeout(Duration.ofSeconds(8))
                            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200 && response.body() != null) {
                        String body = response.body();
                        String responseText = extractTextFromGeminiResponse(body);
                        if (responseText != null && !responseText.isBlank()) {
                            int start = responseText.indexOf('{');
                            int end = responseText.lastIndexOf('}');
                            if (start >= 0 && end > start) {
                                JsonNode node = objectMapper.readTree(responseText.substring(start, end + 1));
                                String qText = node.path("question").asText();
                                if (qText != null && qText.trim().length() > 10) {
                                    String tips = node.path("preparation_tips").asText(matched.tips());
                                    return new GeneratedQuestionResult(
                                            qText.trim(),
                                            normCategory,
                                            normDifficulty,
                                            targetSkill,
                                            tips,
                                            true
                                    );
                                }
                            }
                        }
                    }
                } catch (Exception modelEx) {
                    log.warn("Gemini model {} failed during interview question generation: {}", model, modelEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error during Gemini interview question generation execution: {}", e.getMessage());
        }

        return fallbackResult;
    }

    public InterviewScoresDto calculateInterviewScore(double relevance, double clarity,
                                                      double completeness, double technicalUnderstanding) {
        double clampedRel = Math.max(0.0, Math.min(5.0, relevance));
        double clampedCla = Math.max(0.0, Math.min(5.0, clarity));
        double clampedCom = Math.max(0.0, Math.min(5.0, completeness));
        double clampedTec = Math.max(0.0, Math.min(5.0, technicalUnderstanding));

        double total = clampedRel + clampedCla + clampedCom + clampedTec;
        double maxTotal = 20.0;
        double percentage = Math.round(((total / maxTotal) * 100.0) * 100.0) / 100.0;

        return new InterviewScoresDto(
                clampedRel,
                clampedCla,
                clampedCom,
                clampedTec,
                total,
                maxTotal,
                percentage,
                "(relevance + clarity + completeness + technical_understanding) / 20 * 100"
        );
    }

    public record EvaluationResult(
            InterviewScoresDto scores,
            InterviewFeedbackDto feedback
    ) {}

    public EvaluationResult generateFallbackEvaluation(String question, String answer, String category,
                                                       String difficulty, String targetSkill) {
        int wordCount = (answer != null) ? answer.trim().split("\\s+").length : 0;
        double relevance = 3.0;
        double clarity = 3.0;
        double completeness = 3.0;
        double technical = 3.0;

        if (wordCount > 60) {
            relevance = 4.0;
            clarity = 4.0;
            completeness = 4.0;
            technical = 4.0;
        } else if (wordCount < 15) {
            relevance = 2.0;
            clarity = 2.0;
            completeness = 2.0;
            technical = 2.0;
        }

        InterviewScoresDto scores = calculateInterviewScore(relevance, clarity, completeness, technical);

        InterviewFeedbackDto feedback = new InterviewFeedbackDto(
                List.of(
                        "Attempted to address the interview prompt directly.",
                        wordCount > 40 ? "Provided adequate length and introductory context." : "Concise response."
                ),
                List.of(
                        "Elaborate with specific technical mechanisms or concrete project trade-offs.",
                        "Use the STAR method (Situation, Task, Action, Result) for structured storytelling."
                ),
                "Your response shows an initial understanding of " + (targetSkill != null ? targetSkill : "the topic")
                        + ". To score higher, deepen the explanation of underlying implementation details.",
                "Practice answering with a specific code example or architectural trade-off relating to "
                        + (targetSkill != null ? targetSkill : "this skill") + ".",
                false
        );

        return new EvaluationResult(scores, feedback);
    }

    public EvaluationResult evaluateInterviewAnswer(String question, String answer, String category,
                                                    String difficulty, String targetSkill, String targetRole) {
        EvaluationResult fallback = generateFallbackEvaluation(question, answer, category, difficulty, targetSkill);

        if (apiKey.isEmpty() || "mock_key".equalsIgnoreCase(apiKey) || "your_gemini_api_key_here".equalsIgnoreCase(apiKey)) {
            log.info("Gemini API key is unconfigured or mock; using deterministic interview evaluation fallback");
            return fallback;
        }

        if (detectPromptInjection(answer) || detectPromptInjection(question)) {
            log.warn("Prompt injection detected in interview answer evaluation. Utilizing fallback.");
            return fallback;
        }

        try {
            String safeAnswer = (answer != null && answer.length() > 4000) ? answer.substring(0, 4000) : (answer != null ? answer : "");
            String prompt = """
                    You are a Technical Interview Evaluator.
                    Evaluate the student's interview response objectively.

                    STRICT CONSTRAINTS:
                    1. Rate each dimension as an integer or decimal between 0.0 and 5.0:
                       - relevance (0 to 5): How well does the answer address the specific question?
                       - clarity (0 to 5): How clear, structured, and easy to understand is the explanation?
                       - completeness (0 to 5): Did the student cover the key aspects or edge cases?
                       - technical_understanding (0 to 5): Does the student demonstrate accurate technical concepts?
                    2. Do NOT speculate about the student's personality, IQ, or mental health.
                    3. Label all comments as AI feedback.
                    4. Do NOT invent achievements or external claims.

                    QUESTION:
                    "%s"

                    TARGET ROLE: %s
                    CATEGORY: %s
                    DIFFICULTY: %s
                    TARGET SKILL: %s

                    STUDENT'S ANSWER:
                    "%s"

                    Return JSON strictly in this format:
                    {
                      "relevance": 4.0,
                      "clarity": 4.0,
                      "completeness": 3.5,
                      "technical_understanding": 4.0,
                      "strengths": ["point 1", "point 2"],
                      "improvements": ["point 1", "point 2"],
                      "overall_feedback": "Summary paragraph of feedback...",
                      "follow_up_tip": "One actionable tip to improve in this interview category..."
                    }
                    """.formatted(question, targetRole, category, difficulty, targetSkill, safeAnswer);

            for (String model : GEMINI_MODELS) {
                try {
                    String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
                    Map<String, Object> reqBody = Map.of(
                            "contents", List.of(
                                    Map.of("parts", List.of(Map.of("text", prompt)))
                            )
                    );
                    String jsonPayload = objectMapper.writeValueAsString(reqBody);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .header("Content-Type", "application/json")
                            .timeout(Duration.ofSeconds(8))
                            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200 && response.body() != null) {
                        String body = response.body();
                        String responseText = extractTextFromGeminiResponse(body);
                        if (responseText != null && !responseText.isBlank()) {
                            int start = responseText.indexOf('{');
                            int end = responseText.lastIndexOf('}');
                            if (start >= 0 && end > start) {
                                JsonNode node = objectMapper.readTree(responseText.substring(start, end + 1));
                                double rel = node.path("relevance").asDouble(fallback.scores().relevance());
                                double cla = node.path("clarity").asDouble(fallback.scores().clarity());
                                double com = node.path("completeness").asDouble(fallback.scores().completeness());
                                double tec = node.path("technical_understanding").asDouble(fallback.scores().technicalUnderstanding());

                                InterviewScoresDto scores = calculateInterviewScore(rel, cla, com, tec);

                                List<String> strengths = extractStringList(node.path("strengths"), fallback.feedback().strengths());
                                List<String> improvements = extractStringList(node.path("improvements"), fallback.feedback().improvements());
                                String overall = node.path("overall_feedback").asText(fallback.feedback().overallFeedback());
                                String tip = node.path("follow_up_tip").asText(fallback.feedback().followUpTip());

                                InterviewFeedbackDto feedback = new InterviewFeedbackDto(
                                        strengths,
                                        improvements,
                                        overall,
                                        tip,
                                        true
                                );

                                return new EvaluationResult(scores, feedback);
                            }
                        }
                    }
                } catch (Exception modelEx) {
                    log.warn("Gemini model {} failed during interview evaluation: {}", model, modelEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error during Gemini interview evaluation execution: {}", e.getMessage());
        }

        return fallback;
    }

    public InterviewHistoryDataDto analyzeInterviewWeakAreas(List<InterviewPractice> practices) {
        if (practices == null || practices.isEmpty()) {
            return new InterviewHistoryDataDto(
                    Collections.emptyList(),
                    0,
                    0.0,
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptyList(),
                    Collections.emptyList()
            );
        }

        Map<String, double[]> categoryMap = new LinkedHashMap<>(); // [attempts, total_percentage]
        Map<String, double[]> skillMap = new LinkedHashMap<>();
        double totalScoreSum = 0.0;

        List<InterviewPracticeDto> historyList = new ArrayList<>();

        for (InterviewPractice p : practices) {
            historyList.add(p.toDto());
            String cat = p.getCategory() != null ? p.getCategory() : "TECHNICAL";
            String skill = p.getTargetSkill() != null ? p.getTargetSkill() : "General";
            double percentage = p.getScores() != null ? p.getScores().percentage() : 0.0;

            totalScoreSum += percentage;

            categoryMap.computeIfAbsent(cat, k -> new double[]{0.0, 0.0});
            categoryMap.get(cat)[0] += 1.0;
            categoryMap.get(cat)[1] += percentage;

            skillMap.computeIfAbsent(skill, k -> new double[]{0.0, 0.0});
            skillMap.get(skill)[0] += 1.0;
            skillMap.get(skill)[1] += percentage;
        }

        Map<String, InterviewHistoryDataDto.CategoryBreakdownDto> catBreakdown = new LinkedHashMap<>();
        List<InterviewHistoryDataDto.WeakAreaDto> weakAreas = new ArrayList<>();
        List<InterviewHistoryDataDto.StrongAreaDto> strongAreas = new ArrayList<>();

        for (Map.Entry<String, double[]> entry : categoryMap.entrySet()) {
            String cat = entry.getKey();
            int attempts = (int) entry.getValue()[0];
            double avg = Math.round((entry.getValue()[1] / attempts) * 100.0) / 100.0;

            catBreakdown.put(cat, new InterviewHistoryDataDto.CategoryBreakdownDto(attempts, avg));

            if (avg < 70.0) {
                weakAreas.add(new InterviewHistoryDataDto.WeakAreaDto(
                        cat,
                        avg,
                        "NEEDS_IMPROVEMENT",
                        "Average performance is below target 70%. Focus on additional " + cat.toLowerCase() + " practice."
                ));
            } else {
                strongAreas.add(new InterviewHistoryDataDto.StrongAreaDto(
                        cat,
                        avg,
                        "PROFICIENT",
                        "Solid demonstration of " + cat.toLowerCase() + " competencies."
                ));
            }
        }

        Map<String, InterviewHistoryDataDto.SkillBreakdownDto> skillBreakdown = new LinkedHashMap<>();
        for (Map.Entry<String, double[]> entry : skillMap.entrySet()) {
            String skill = entry.getKey();
            int attempts = (int) entry.getValue()[0];
            double avg = Math.round((entry.getValue()[1] / attempts) * 100.0) / 100.0;
            skillBreakdown.put(skill, new InterviewHistoryDataDto.SkillBreakdownDto(attempts, avg));
        }

        weakAreas.sort(Comparator.comparingDouble(InterviewHistoryDataDto.WeakAreaDto::averagePercentage));

        double averageScore = Math.round((totalScoreSum / practices.size()) * 100.0) / 100.0;

        return new InterviewHistoryDataDto(
                historyList,
                practices.size(),
                averageScore,
                catBreakdown,
                skillBreakdown,
                weakAreas,
                strongAreas
        );
    }

    private String extractTextFromGeminiResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response tree: {}", e.getMessage());
        }
        return null;
    }

    private List<String> extractStringList(JsonNode node, List<String> fallback) {
        if (node != null && node.isArray() && !node.isEmpty()) {
            List<String> list = new ArrayList<>();
            for (JsonNode item : node) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    list.add(item.asText().trim());
                }
            }
            if (!list.isEmpty()) {
                return list;
            }
        }
        return fallback;
    }
}
