package com.collegecopilot.career;

import com.collegecopilot.career.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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
public class GeminiCareerAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(GeminiCareerAdvisorService.class);

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

    public GeminiCareerAdvisorService() {
        this("");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiCareerAdvisorService(@Value("${GEMINI_API_KEY:${AI_API_KEY:}}") String apiKey) {
        this(apiKey, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    public GeminiCareerAdvisorService(String apiKey, HttpClient httpClient) {
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

    private boolean containsInjection(CareerProfile profile) {
        if (profile == null) return false;
        if (detectPromptInjection(profile.getCareerGoal())) return true;
        if (detectPromptInjection(profile.getDomain())) return true;
        if (detectPromptInjection(profile.getTargetRole())) return true;
        if (profile.getInterests() != null && profile.getInterests().stream().anyMatch(this::detectPromptInjection)) return true;
        if (profile.getSkills() != null && profile.getSkills().stream().anyMatch(s -> s != null && detectPromptInjection(s.getSkill()))) return true;
        return false;
    }

    public CareerAdvisorDataDto generateCareerAdvice(CareerProfile careerProfile,
                                                     SkillGapAnalysisDto skillGapAnalysis,
                                                     List<String> weakQuizTopics) {
        String targetRole = skillGapAnalysis.getTargetRole() != null ? skillGapAnalysis.getTargetRole() : "Full Stack Developer";
        double coverage = skillGapAnalysis.getCoveragePercentage();

        if (apiKey.isEmpty() || "mock_key".equalsIgnoreCase(apiKey) || "your_gemini_api_key_here".equalsIgnoreCase(apiKey)) {
            log.info("Gemini API key is unconfigured or mock; using deterministic career advisory fallback");
            return generateFallbackCareerAdvice(skillGapAnalysis, careerProfile);
        }

        if (containsInjection(careerProfile)) {
            log.warn("Prompt injection detected in career profile; reverting safely to deterministic advisory");
            return generateFallbackCareerAdvice(skillGapAnalysis, careerProfile);
        }

        String prompt = buildAdvisorPrompt(careerProfile, skillGapAnalysis, weakQuizTopics);

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
                            "temperature": 0.4,
                            "topK": 40,
                            "topP": 0.95,
                            "maxOutputTokens": 1024
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
                        return new CareerAdvisorDataDto(
                                String.format("AI Career Strategy for %s (%.2f%% coverage).", targetRole, coverage),
                                extracted.trim(),
                                skillGapAnalysis,
                                false
                        );
                    }
                }
            } catch (Exception e) {
                lastError = e;
                log.debug("Gemini model {} failed during career advice generation: {}", model, e.getMessage());
            }
        }

        log.warn("Gemini call failed or timed out across models, using deterministic advice fallback: {}",
                lastError != null ? lastError.getMessage() : "empty reply");
        return generateFallbackCareerAdvice(skillGapAnalysis, careerProfile);
    }

    public CareerRoadmapDataDto generateLearningRoadmap(CareerProfile careerProfile,
                                                        SkillGapAnalysisDto skillGapAnalysis) {
        String targetRole = skillGapAnalysis.getTargetRole() != null ? skillGapAnalysis.getTargetRole() : "Full Stack Developer";
        List<String> missingList = skillGapAnalysis.getMissingList() != null ? skillGapAnalysis.getMissingList() : List.of();
        List<String> partialList = skillGapAnalysis.getPartialList() != null ? skillGapAnalysis.getPartialList() : List.of();

        List<String> gapsToClose = new ArrayList<>(missingList);
        gapsToClose.addAll(partialList);

        if (gapsToClose.isEmpty()) {
            return new CareerRoadmapDataDto(
                    "🎉 High Readiness! You meet all core skill requirements for " + targetRole + ".",
                    List.of(new RoadmapPhaseDto(
                            "Phase 1: Master-level Refinement",
                            List.of("Advanced System Architecture", "Performance Optimization"),
                            "HIGH",
                            "Build full-stack open-source projects & practice mock system design interviews.",
                            "3-4 weeks"
                    )),
                    false
            );
        }

        if (apiKey.isEmpty() || "mock_key".equalsIgnoreCase(apiKey) || "your_gemini_api_key_here".equalsIgnoreCase(apiKey)) {
            log.info("Gemini API key is unconfigured or mock; using deterministic roadmap fallback");
            return generateFallbackRoadmap(targetRole, missingList, partialList);
        }

        if (containsInjection(careerProfile)) {
            log.warn("Prompt injection detected in career profile; reverting safely to deterministic roadmap");
            return generateFallbackRoadmap(targetRole, missingList, partialList);
        }

        String prompt = buildRoadmapPrompt(targetRole, gapsToClose);

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
                            "temperature": 0.2,
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
                        CareerRoadmapDataDto parsed = parseStructuredRoadmap(extracted);
                        if (parsed != null && parsed.getPhases() != null && !parsed.getPhases().isEmpty()) {
                            parsed.setFallback(false);
                            return parsed;
                        }
                    }
                }
            } catch (Exception e) {
                lastError = e;
                log.debug("Gemini model {} failed during roadmap generation: {}", model, e.getMessage());
            }
        }

        log.warn("Gemini roadmap call failed across models, using deterministic roadmap fallback: {}",
                lastError != null ? lastError.getMessage() : "empty reply");
        return generateFallbackRoadmap(targetRole, missingList, partialList);
    }

    public CareerAdvisorDataDto generateFallbackCareerAdvice(SkillGapAnalysisDto skillGapAnalysis,
                                                             CareerProfile careerProfile) {
        String targetRole = skillGapAnalysis.getTargetRole() != null ? skillGapAnalysis.getTargetRole() : "Full Stack Developer";
        List<String> missingList = skillGapAnalysis.getMissingList() != null ? skillGapAnalysis.getMissingList() : List.of();
        List<String> partialList = skillGapAnalysis.getPartialList() != null ? skillGapAnalysis.getPartialList() : List.of();
        double coverage = skillGapAnalysis.getCoveragePercentage();

        String recommendation;
        if (!missingList.isEmpty()) {
            String m0 = missingList.get(0);
            String m1 = missingList.size() > 1 ? missingList.get(1) : m0;
            recommendation = String.format(
                    "🎯 **SKILL GAP PRIORITIZATION**: For your target role as **%s**, your top priority should be acquiring **%s** and **%s**. Current skill coverage is %.2f%%.",
                    targetRole, m0, m1, coverage
            );
        } else if (!partialList.isEmpty()) {
            recommendation = String.format(
                    "⚡ **PROFICIENCY UPGRADE**: You have basic exposure to **%s**. Advance these skills from Beginner to Intermediate to improve role readiness.",
                    String.join(", ", partialList)
            );
        } else {
            recommendation = String.format(
                    "🌟 **HIGH READINESS**: You meet the skill requirements for %s! Focus on building comprehensive portfolio projects.",
                    targetRole
            );
        }

        return new CareerAdvisorDataDto(
                "Deterministic Skill Gap Analysis for " + targetRole + ".",
                recommendation,
                skillGapAnalysis,
                true
        );
    }

    public CareerRoadmapDataDto generateFallbackRoadmap(String targetRole,
                                                        List<String> missingList,
                                                        List<String> partialList) {
        List<String> gaps = new ArrayList<>();
        if (missingList != null) gaps.addAll(missingList);
        if (partialList != null) gaps.addAll(partialList);

        List<String> phase1Skills = gaps.size() > 2 ? gaps.subList(0, 2) : new ArrayList<>(gaps);
        List<String> phase2Skills = gaps.size() > 2 ? (gaps.size() > 4 ? gaps.subList(2, 4) : gaps.subList(2, gaps.size())) : List.of();
        List<String> phase3Skills = gaps.size() > 4 ? gaps.subList(4, gaps.size()) : List.of();

        List<RoadmapPhaseDto> phases = new ArrayList<>();

        if (!phase1Skills.isEmpty()) {
            phases.add(new RoadmapPhaseDto(
                    "Phase 1: High Priority Foundations",
                    phase1Skills,
                    "HIGH",
                    "Master fundamentals and practical exercises in " + String.join(" and ", phase1Skills) + ".",
                    "2-3 weeks"
            ));
        }

        if (!phase2Skills.isEmpty()) {
            phases.add(new RoadmapPhaseDto(
                    "Phase 2: Core Competencies",
                    phase2Skills,
                    "MEDIUM",
                    "Build API & database features using " + String.join(" and ", phase2Skills) + ".",
                    "3-4 weeks"
            ));
        }

        if (!phase3Skills.isEmpty()) {
            phases.add(new RoadmapPhaseDto(
                    "Phase 3: Integration & Deployment",
                    phase3Skills,
                    "MEDIUM",
                    "Integrate " + String.join(" and ", phase3Skills) + " into full-stack project portfolio.",
                    "2-3 weeks"
            ));
        }

        return new CareerRoadmapDataDto(
                "Deterministic 3-Phase Learning Roadmap for " + targetRole + ".",
                phases,
                true
        );
    }

    private String buildAdvisorPrompt(CareerProfile careerProfile,
                                       SkillGapAnalysisDto skillGapAnalysis,
                                       List<String> weakQuizTopics) {
        String targetRole = skillGapAnalysis.getTargetRole() != null ? skillGapAnalysis.getTargetRole() : "Full Stack Developer";
        String goal = careerProfile.getCareerGoal() != null ? careerProfile.getCareerGoal() : targetRole;
        String domain = careerProfile.getDomain() != null ? careerProfile.getDomain() : "Software Development";
        double coverage = skillGapAnalysis.getCoveragePercentage();

        List<String> strongList = skillGapAnalysis.getStrongList() != null ? skillGapAnalysis.getStrongList() : List.of();
        List<String> partialList = skillGapAnalysis.getPartialList() != null ? skillGapAnalysis.getPartialList() : List.of();
        List<String> missingList = skillGapAnalysis.getMissingList() != null ? skillGapAnalysis.getMissingList() : List.of();
        List<String> weak = weakQuizTopics != null ? weakQuizTopics : List.of();

        List<String> topStrong = strongList.size() > 3 ? strongList.subList(0, 3) : strongList;

        return String.format("""
                You are an expert AI Career & Skill Advisor for college software engineers.
                Analyze the logged-in student's AUTHORITATIVE career profile, skill gap calculations, and quiz performance.

                STRICT RULE: Only reference actual skills, target roles, and quiz topics provided below. Do NOT invent fake projects, skills, or job offers.

                CAREER PROFILE:
                - Goal: %s
                - Target Role: %s
                - Domain: %s
                - Current Skill Coverage: %.2f%%

                DETERMINISTIC SKILL GAP ANALYSIS:
                - Strong Skills (>=Intermediate): %s
                - Partial Skills (Beginner): %s
                - Missing Critical Skills (Gaps): %s

                ACADEMIC WEAK TOPICS:
                %s

                Answer the following questions concisely:
                1. What technical skills should the student focus on learning NEXT?
                2. Which missing skill gaps are most critical for their role as %s?
                3. How can they leverage their existing strong skills (%s)?
                4. Recommended practical project idea incorporating their missing skills.
                """,
                goal,
                targetRole,
                domain,
                coverage,
                !strongList.isEmpty() ? String.join(", ", strongList) : "None",
                !partialList.isEmpty() ? String.join(", ", partialList) : "None",
                !missingList.isEmpty() ? String.join(", ", missingList) : "None",
                !weak.isEmpty() ? String.join(", ", weak) : "None (<60%)",
                targetRole,
                !topStrong.isEmpty() ? String.join(", ", topStrong) : "core skills"
        );
    }

    private String buildRoadmapPrompt(String targetRole, List<String> gapsToClose) {
        String firstGap = !gapsToClose.isEmpty() ? gapsToClose.get(0) : "Core Skill";
        return String.format("""
                You are an expert AI Career Roadmap Architect.
                Given a student's target role and deterministic skill gaps, construct a 3-phase structured learning roadmap.

                STRICT RULE: Do NOT invent fake course URLs or external job offers. Focus purely on technical learning goals.

                TARGET ROLE: %s
                IDENTIFIED SKILL GAPS TO CLOSE: %s

                STRICT JSON OUTPUT REQUIREMENT:
                Return ONLY a valid JSON object (no markdown, no extra text):
                {
                  "summary": "Short 2-sentence summary of the roadmap strategy for %s.",
                  "phases": [
                    {
                      "phase": "Phase 1: Foundations",
                      "skills": ["%s"],
                      "priority": "HIGH",
                      "focus": "Key concepts and hands-on exercises for %s.",
                      "effort": "2-3 weeks"
                    }
                  ]
                }
                """,
                targetRole,
                String.join(", ", gapsToClose),
                targetRole,
                firstGap,
                firstGap
        );
    }

    private CareerRoadmapDataDto parseStructuredRoadmap(String jsonText) {
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

            return objectMapper.readValue(clean, CareerRoadmapDataDto.class);
        } catch (Exception e) {
            log.debug("Failed to parse learning roadmap JSON: {}", e.getMessage());
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
