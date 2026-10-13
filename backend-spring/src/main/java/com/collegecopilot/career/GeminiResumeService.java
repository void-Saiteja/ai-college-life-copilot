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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GeminiResumeService {

    private static final Logger log = LoggerFactory.getLogger(GeminiResumeService.class);

    public static final List<String> KNOWN_TECH_SKILLS = List.of(
            "JavaScript", "TypeScript", "Python", "Java", "C++", "C#", "Go", "Rust", "PHP", "Ruby", "Swift", "Kotlin",
            "React", "Vue", "Angular", "Next.js", "Node.js", "Express", "Django", "Flask", "FastAPI", "Spring Boot",
            "HTML", "CSS", "Tailwind", "Bootstrap", "Sass", "Redux", "GraphQL",
            "SQL", "PostgreSQL", "MySQL", "MongoDB", "Redis", "SQLite", "Oracle",
            "REST APIs", "APIs", "Microservices", "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Linux", "Git", "CI/CD",
            "Machine Learning", "Deep Learning", "PyTorch", "TensorFlow", "NLP", "Computer Vision", "Data Visualization",
            "Pandas", "NumPy", "Scikit-Learn", "Statistics", "Excel", "Tableau", "PowerBI"
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

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public GeminiResumeService() {
        this("");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiResumeService(@Value("${GEMINI_API_KEY:${AI_API_KEY:}}") String apiKey) {
        this(apiKey, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    public GeminiResumeService(String apiKey, HttpClient httpClient) {
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

    public List<ResumeDetectedSkillDto> extractSkillsFromResume(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) {
            return Collections.emptyList();
        }

        Set<String> allCatalogSkills = new LinkedHashSet<>(KNOWN_TECH_SKILLS);
        CareerService.ROLE_SKILL_CATALOG.values().forEach(allCatalogSkills::addAll);

        List<ResumeDetectedSkillDto> detected = new ArrayList<>();
        String textLower = resumeText.toLowerCase();

        for (String skill : allCatalogSkills) {
            Pattern pattern;
            String skillLower = skill.toLowerCase();

            if ("c++".equalsIgnoreCase(skill)) {
                pattern = Pattern.compile("(?:^|\\s|\\W)c\\+\\+(?:$|\\s|\\W)", Pattern.CASE_INSENSITIVE);
            } else if ("c#".equalsIgnoreCase(skill)) {
                pattern = Pattern.compile("(?:^|\\s|\\W)c#(?:$|\\s|\\W)", Pattern.CASE_INSENSITIVE);
            } else if ("rest apis".equalsIgnoreCase(skillLower) || "apis".equalsIgnoreCase(skillLower)) {
                pattern = Pattern.compile("\\b(rest(ful)?\\s*apis?|apis?)\\b", Pattern.CASE_INSENSITIVE);
            } else if ("node.js".equalsIgnoreCase(skillLower)) {
                pattern = Pattern.compile("\\bnode(\\.js)?\\b", Pattern.CASE_INSENSITIVE);
            } else if ("react".equalsIgnoreCase(skillLower)) {
                pattern = Pattern.compile("\\breact(\\.js)?\\b", Pattern.CASE_INSENSITIVE);
            } else {
                String escaped = Pattern.quote(skill);
                pattern = Pattern.compile("(^|[^a-zA-Z0-9_])" + escaped + "([^a-zA-Z0-9_]|$)", Pattern.CASE_INSENSITIVE);
            }

            if (pattern.matcher(textLower).find()) {
                detected.add(new ResumeDetectedSkillDto(skill, "DETECTED_IN_RESUME"));
            }
        }

        return detected;
    }

    public ResumeCoverageDto calculateResumeCoverage(List<ResumeDetectedSkillDto> detectedSkills, String targetRole) {
        String role = (targetRole != null && CareerService.ROLE_SKILL_CATALOG.containsKey(targetRole))
                ? targetRole
                : "Full Stack Developer";

        List<String> requiredSkills = CareerService.ROLE_SKILL_CATALOG.getOrDefault(role,
                CareerService.ROLE_SKILL_CATALOG.get("Full Stack Developer"));
        int totalRequired = requiredSkills.size();

        Set<String> detectedSkillNames = (detectedSkills != null ? detectedSkills : Collections.<ResumeDetectedSkillDto>emptyList())
                .stream()
                .map(d -> d.skill().toLowerCase().trim())
                .collect(Collectors.toSet());

        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (String req : requiredSkills) {
            if (detectedSkillNames.contains(req.toLowerCase().trim())) {
                matchedSkills.add(req);
            } else {
                missingSkills.add(req);
            }
        }

        int matchedCount = matchedSkills.size();
        int missingCount = missingSkills.size();
        double coveragePercentage = totalRequired > 0
                ? Math.round((((double) matchedCount / totalRequired) * 100.0) * 100.0) / 100.0
                : 0.0;

        return new ResumeCoverageDto(
                role,
                totalRequired,
                matchedCount,
                missingCount,
                coveragePercentage,
                matchedSkills,
                missingSkills,
                "(detected_required_skills / total_required_skills) * 100"
        );
    }

    public ResumeAnalysisDto generateFallbackResumeAdvice(String targetRole,
                                                          ResumeCoverageDto coverage,
                                                          List<ResumeDetectedSkillDto> detectedSkills,
                                                          CareerProfile careerProfile,
                                                          SkillGapAnalysisDto skillGapAnalysis) {
        List<String> matchedSkills = coverage.matchedSkills() != null ? coverage.matchedSkills() : Collections.emptyList();
        List<String> missingSkills = coverage.missingSkills() != null ? coverage.missingSkills() : Collections.emptyList();

        List<String> strengths = !matchedSkills.isEmpty()
                ? matchedSkills.stream()
                .map(s -> "Strong representation of required competency '" + s + "' in your resume.")
                .toList()
                : List.of("Resume structure contains readable experience and profile sections.");

        List<String> improvements = !missingSkills.isEmpty()
                ? missingSkills.stream()
                .map(s -> "No explicit evidence detected for required role skill '" + s + "'. Consider adding coursework or projects demonstrating '" + s + "'.")
                .toList()
                : List.of("Solid coverage of core role skills. Focus on quantifying accomplishments and business impact.");

        List<String> suggestions = new ArrayList<>();
        suggestions.add("Target Role Alignment: Emphasize practical experience aligned with " + targetRole + " requirements.");
        if (!missingSkills.isEmpty()) {
            String missingSample = missingSkills.stream().limit(3).collect(Collectors.joining(", "));
            suggestions.add("Skill Clarification: If you have practical experience with " + missingSample + ", explicitly list them with project context.");
        } else {
            suggestions.add("Highlight architectural decisions and performance optimizations.");
        }
        suggestions.add("Impact & Metrics: Add quantifiable outcomes (e.g. % performance increase, query speedup, user scale) only where you have factual project data.");
        suggestions.add("Section Clarity: Ensure distinct sections for Summary, Technical Skills, Projects, and Education.");

        ResumeDocumentedFactsDto facts = new ResumeDocumentedFactsDto(
                targetRole,
                detectedSkills != null ? detectedSkills.size() : 0,
                coverage.coveragePercentage(),
                matchedSkills,
                missingSkills
        );

        ResumeAiSuggestionsDto aiSuggestions = new ResumeAiSuggestionsDto(
                strengths,
                improvements,
                suggestions,
                false,
                "Deterministic resume analysis based on role skill catalog and verified text parsing."
        );

        return new ResumeAnalysisDto(
                targetRole,
                coverage,
                detectedSkills,
                skillGapAnalysis,
                facts,
                aiSuggestions
        );
    }

    public ResumeAnalysisDto analyzeResume(String resumeText,
                                           String targetRole,
                                           CareerProfile careerProfile,
                                           SkillGapAnalysisDto skillGapAnalysis) {
        String role = (targetRole != null && CareerService.ROLE_SKILL_CATALOG.containsKey(targetRole))
                ? targetRole
                : "Full Stack Developer";

        List<ResumeDetectedSkillDto> detectedSkills = extractSkillsFromResume(resumeText);
        ResumeCoverageDto coverage = calculateResumeCoverage(detectedSkills, role);

        ResumeAnalysisDto fallback = generateFallbackResumeAdvice(role, coverage, detectedSkills, careerProfile, skillGapAnalysis);

        if (apiKey.isEmpty() || "mock_key".equalsIgnoreCase(apiKey) || "your_gemini_api_key_here".equalsIgnoreCase(apiKey)) {
            log.info("Gemini API key is unconfigured or mock; using deterministic resume fallback");
            return fallback;
        }

        if (detectPromptInjection(resumeText) || detectPromptInjection(role)) {
            log.warn("Prompt injection detected in resume analysis request. Utilizing deterministic fallback.");
            return fallback;
        }

        try {
            String safeText = (resumeText != null && resumeText.length() > 4000) ? resumeText.substring(0, 4000) : (resumeText != null ? resumeText : "");

            List<String> reqSkills = CareerService.ROLE_SKILL_CATALOG.getOrDefault(role, Collections.emptyList());
            String matchedStr = coverage.matchedSkills().isEmpty() ? "None" : String.join(", ", coverage.matchedSkills());
            String missingStr = coverage.missingSkills().isEmpty() ? "None" : String.join(", ", coverage.missingSkills());

            String prompt = """
                    You are an expert Technical Resume Reviewer and Career Coach.
                    Analyze the following student resume for the target role: "%s".

                    CRITICAL INSTRUCTIONS & ANTI-HALLUCINATION RULES:
                    1. Do NOT invent fake projects, fake internships, fake companies, fake degrees, or fake metrics.
                    2. Only suggest improvements based on the student's actual written resume text.
                    3. Recommend adding skills or metrics ONLY IF the student genuinely possesses that experience.
                    4. Clearly provide constructive feedback.

                    DETERMINISTIC ANALYSIS CONTEXT:
                    - Target Role: %s
                    - Required Skills: %s
                    - Skills Detected in Resume: %s
                    - Missing Required Skills: %s
                    - Deterministic Coverage: %.2f%%

                    STUDENT RESUME TEXT:
                    \"\"\"
                    %s
                    \"\"\"

                    Provide your review as clean JSON with this exact structure:
                    {
                      "strengths": ["bullet point 1", "bullet point 2"],
                      "improvements": ["bullet point 1", "bullet point 2"],
                      "actionableSuggestions": [
                        "actionable suggestion 1",
                        "actionable suggestion 2",
                        "actionable suggestion 3"
                      ]
                    }
                    """.formatted(role, role, String.join(", ", reqSkills), matchedStr, missingStr, coverage.coveragePercentage(), safeText);

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
                            ResumeAiSuggestionsDto aiSuggestions = parseAiSuggestions(responseText, fallback.aiSuggestions());
                            if (aiSuggestions != null) {
                                return new ResumeAnalysisDto(
                                        role,
                                        coverage,
                                        detectedSkills,
                                        skillGapAnalysis,
                                        fallback.documentedFacts(),
                                        aiSuggestions
                                );
                            }
                        }
                    }
                } catch (Exception modelEx) {
                    log.warn("Gemini model {} failed during resume analysis: {}", model, modelEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error during Gemini resume analysis execution: {}", e.getMessage());
        }

        return fallback;
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

    private ResumeAiSuggestionsDto parseAiSuggestions(String rawText, ResumeAiSuggestionsDto fallback) {
        try {
            String text = rawText.trim();
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start >= 0 && end > start) {
                String jsonStr = text.substring(start, end + 1);
                JsonNode node = objectMapper.readTree(jsonStr);

                List<String> strengths = extractStringList(node.path("strengths"), fallback.strengths());
                List<String> improvements = extractStringList(node.path("improvements"), fallback.improvements());
                List<String> actionable = extractStringList(node.path("actionableSuggestions"), fallback.actionableSuggestions());

                return new ResumeAiSuggestionsDto(
                        strengths,
                        improvements,
                        actionable,
                        true,
                        "AI-generated qualitative suggestions. Verify all suggestions against your genuine academic and project experience."
                );
            }
        } catch (Exception e) {
            log.warn("Failed to parse AI suggestions JSON: {}", e.getMessage());
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
