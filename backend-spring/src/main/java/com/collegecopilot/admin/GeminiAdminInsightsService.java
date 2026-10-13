package com.collegecopilot.admin;

import com.collegecopilot.admin.dto.AdminDashboardDataDto;
import com.collegecopilot.admin.dto.AdminInsightsDataDto;
import com.collegecopilot.admin.dto.AdminRisksDataDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GeminiAdminInsightsService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAdminInsightsService.class);

    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("\\{[\\s\\S]*\\}");
    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final List<String> GEMINI_MODELS = List.of(
            "gemini-1.5-flash",
            "gemini-2.0-flash",
            "gemini-2.5-flash"
    );

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    public GeminiAdminInsightsService() {
        this("");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiAdminInsightsService(@Value("${GEMINI_API_KEY:${AI_API_KEY:}}") String apiKey) {
        this(apiKey, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    public GeminiAdminInsightsService(String apiKey, HttpClient httpClient) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.httpClient = httpClient;
    }

    public AdminInsightsDataDto generateInsights(AdminDashboardDataDto stats, AdminRisksDataDto risks) {
        AdminInsightsDataDto fallback = generateDeterministicFallbackInsights(stats, risks);

        if (apiKey.isBlank() || "mock_key".equalsIgnoreCase(apiKey) ||
                "your_gemini_api_key_here".equalsIgnoreCase(apiKey)) {
            return fallback;
        }

        try {
            String prompt = buildPrompt(stats, risks);

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
                        String rawText = extractTextFromGeminiResponse(response.body());
                        if (rawText != null && !rawText.isBlank()) {
                            AdminInsightsDataDto parsed = parseInsightsResponse(rawText, fallback);
                            if (parsed != null) {
                                parsed.setIsAiGenerated(true);
                                return parsed;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Gemini model {} failed during admin insights generation: {}", model, e.getMessage());
                }
            }
        } catch (Exception ex) {
            log.error("[Admin AI Insights Error]: {}", ex.getMessage());
        }

        return fallback;
    }

    public AdminInsightsDataDto generateDeterministicFallbackInsights(AdminDashboardDataDto stats, AdminRisksDataDto risks) {
        int totalStudents = stats != null && stats.getStudents() != null ? stats.getStudents().getTotalStudents() : 0;
        int activeStudents = stats != null && stats.getStudents() != null ? stats.getStudents().getActiveStudents() : 0;
        double avgAttendance = stats != null && stats.getAttendance() != null ? stats.getAttendance().getAverageAttendance() : 0.0;
        int belowTarget = stats != null && stats.getAttendance() != null ? stats.getAttendance().getStudentsBelowAttendanceTarget() : 0;
        int pendingAsg = stats != null && stats.getAssignments() != null ? stats.getAssignments().getPendingAssignments() : 0;
        int overdueAsg = stats != null && stats.getAssignments() != null ? stats.getAssignments().getOverdueAssignments() : 0;
        int totalAsg = stats != null && stats.getAssignments() != null ? stats.getAssignments().getTotalAssignments() : 0;
        int upcomingExams = stats != null && stats.getExams() != null ? stats.getExams().getUpcomingExams() : 0;
        int examsToday = stats != null && stats.getExams() != null ? stats.getExams().getExamsToday() : 0;
        int studentsWithGaps = stats != null && stats.getCareer() != null ? stats.getCareer().getStudentsWithSkillGaps() : 0;

        List<AdminDashboardDataDto.SkillGapCount> commonGaps = stats != null && stats.getCareer() != null && stats.getCareer().getCommonSkillGaps() != null
                ? stats.getCareer().getCommonSkillGaps() : List.of();

        List<String> observations = List.of(
                String.format("Enrollment status: %d total students enrolled (%d active).", totalStudents, activeStudents),
                String.format("Campus-wide attendance average is %.2f%%. %d student(s) fall below the 75%% mandatory threshold.", avgAttendance, belowTarget),
                String.format("Assignment load: %d pending assignments with %d overdue submission(s).", pendingAsg, overdueAsg),
                String.format("Examination readiness: %d scheduled exam(s), with %d exam(s) scheduled for today.", upcomingExams, examsToday),
                String.format("Technical career readiness: %d student(s) have identified competency gaps in target roles.", studentsWithGaps)
        );

        List<String> attention = new ArrayList<>();
        int attRiskCount = risks != null && risks.getAttendanceRisk() != null ? risks.getAttendanceRisk().size() : 0;
        int asgRiskCount = risks != null && risks.getAssignmentRisk() != null ? risks.getAssignmentRisk().size() : 0;
        int examRiskCount = risks != null && risks.getExamRisk() != null ? risks.getExamRisk().size() : 0;

        if (attRiskCount > 0) {
            attention.add(String.format("%d student(s) currently breach the 75%% attendance policy and require immediate academic warning letters.", attRiskCount));
        }
        if (asgRiskCount > 0) {
            attention.add(String.format("%d student(s) have overdue assignment deadlines requiring departmental intervention.", asgRiskCount));
        }
        if (examRiskCount > 0) {
            attention.add(String.format("%d student(s) have exams scheduled today or within 3 days.", examRiskCount));
        }
        if (attention.isEmpty()) {
            attention.add("All enrolled students currently meet baseline academic engagement benchmarks.");
        }

        List<String> actions = List.of(
                "Issue automated notifications to students whose attendance is in CRITICAL warning status (<75%).",
                "Coordinate with course instructors regarding overdue assignments and offer tutoring assistance.",
                "Schedule mock technical interview sessions for students with high skill gaps in target engineering roles.",
                "Ensure RAG college policy documents remain up-to-date with current semester examination schedules."
        );

        String gapsFormatted = commonGaps.stream()
                .map(g -> String.format("%s (%d)", g.getSkill(), g.getCount()))
                .collect(Collectors.joining(", "));
        if (gapsFormatted.isBlank()) {
            gapsFormatted = "None recorded";
        }

        List<String> patterns = List.of(
                String.format("Course subject catalog contains %d practical project assessments.", totalAsg),
                String.format("Most common industry skill gaps across profiles: %s.", gapsFormatted)
        );

        return new AdminInsightsDataDto(
                observations,
                attention,
                patterns,
                actions,
                "Deterministic summary generated directly from active database records without predictive extrapolation.",
                false
        );
    }

    private String buildPrompt(AdminDashboardDataDto stats, AdminRisksDataDto risks) {
        String statsJson;
        try {
            statsJson = objectMapper.writeValueAsString(stats);
        } catch (Exception e) {
            statsJson = "{}";
        }

        int attRisk = risks != null && risks.getAttendanceRisk() != null ? risks.getAttendanceRisk().size() : 0;
        int asgRisk = risks != null && risks.getAssignmentRisk() != null ? risks.getAssignmentRisk().size() : 0;
        int examRisk = risks != null && risks.getExamRisk() != null ? risks.getExamRisk().size() : 0;
        int quizRisk = risks != null && risks.getQuizRisk() != null ? risks.getQuizRisk().size() : 0;
        int careerGap = risks != null && risks.getCareerGap() != null ? risks.getCareerGap().size() : 0;

        return String.format("""
                You are the Lead Academic Administrator & Intelligence Advisor for a university college copilot system.
                Analyze the following AUTHORITATIVE campus academic statistics and risk metrics.

                STRICT CONSTRAINTS & ANTI-HALLUCINATION RULES:
                1. ONLY reference the exact numbers and categories provided in the JSON data below.
                2. Do NOT invent student names, unlisted percentages, nonexistent trends, or fictitious departments.
                3. Keep suggestions actionable, practical, and strictly advisory.
                4. Output clean valid JSON matching the requested structure.

                DETERMINISTIC CAMPUS STATISTICS:
                %s

                IDENTIFIED STUDENT RISK COUNTS:
                - Attendance Risk (<75%%): %d students
                - Overdue Assignment Risk: %d students
                - Imminent Exam Risk: %d students
                - Weak Quiz Performance (<60%%): %d students
                - Career Skill Gaps: %d students

                Return ONLY valid JSON with this exact structure:
                {
                  "keyObservations": ["point 1", "point 2", "point 3"],
                  "areasRequiringAttention": ["point 1", "point 2"],
                  "notableAcademicPatterns": ["point 1", "point 2"],
                  "suggestedAdministrativeActions": ["action 1", "action 2", "action 3"],
                  "caveatsAndLimitations": "Brief note on current data sample size or limitation."
                }
                """, statsJson, attRisk, asgRisk, examRisk, quizRisk, careerGap);
    }

    private String extractTextFromGeminiResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return null;
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }
        } catch (Exception e) {
            Matcher matcher = TEXT_PATTERN.matcher(responseBody);
            if (matcher.find()) {
                return matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"");
            }
        }
        return null;
    }

    private AdminInsightsDataDto parseInsightsResponse(String rawText, AdminInsightsDataDto fallback) {
        try {
            String jsonContent = rawText.trim();
            Matcher m = JSON_BLOCK_PATTERN.matcher(rawText);
            if (m.find()) {
                jsonContent = m.group(0);
            }
            JsonNode node = objectMapper.readTree(jsonContent);

            List<String> keyObservations = extractStringList(node.path("keyObservations"), fallback.getKeyObservations());
            List<String> areasRequiringAttention = extractStringList(node.path("areasRequiringAttention"), fallback.getAreasRequiringAttention());
            List<String> notableAcademicPatterns = extractStringList(node.path("notableAcademicPatterns"), fallback.getNotableAcademicPatterns());
            List<String> suggestedAdministrativeActions = extractStringList(node.path("suggestedAdministrativeActions"), fallback.getSuggestedAdministrativeActions());
            String caveats = node.has("caveatsAndLimitations") && !node.path("caveatsAndLimitations").asText().isBlank()
                    ? node.path("caveatsAndLimitations").asText()
                    : fallback.getCaveatsAndLimitations();

            return new AdminInsightsDataDto(
                    keyObservations,
                    areasRequiringAttention,
                    notableAcademicPatterns,
                    suggestedAdministrativeActions,
                    caveats,
                    true
            );
        } catch (Exception ex) {
            log.debug("Failed to parse AI insights response JSON: {}", ex.getMessage());
            return fallback;
        }
    }

    private List<String> extractStringList(JsonNode arrayNode, List<String> defaultList) {
        if (arrayNode.isArray() && !arrayNode.isEmpty()) {
            List<String> list = new ArrayList<>();
            for (JsonNode item : arrayNode) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    list.add(item.asText().trim());
                }
            }
            if (!list.isEmpty()) {
                return list;
            }
        }
        return defaultList;
    }

    private String escapeJsonString(String input) {
        if (input == null) return "\"\"";
        try {
            return objectMapper.writeValueAsString(input);
        } catch (Exception e) {
            return "\"" + input.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
        }
    }
}
