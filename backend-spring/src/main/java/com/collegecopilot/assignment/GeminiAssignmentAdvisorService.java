package com.collegecopilot.assignment;

import com.collegecopilot.assignment.dto.AdvisorDataDto;
import com.collegecopilot.assignment.dto.AssignmentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GeminiAssignmentAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAssignmentAdvisorService.class);

    private final String apiKey;
    private final HttpClient httpClient;

    private static final List<String> MODELS = List.of(
            "gemini-1.5-flash",
            "gemini-2.0-flash",
            "gemini-2.5-flash"
    );

    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    public GeminiAssignmentAdvisorService(@Value("${GEMINI_API_KEY:${AI_API_KEY:}}") String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public AdvisorDataDto generatePrioritization(List<AssignmentDto> assignments, List<String> upcomingExams, List<String> attendanceInfo) {
        List<AssignmentDto> pending = assignments.stream()
                .filter(a -> !"COMPLETED".equalsIgnoreCase(a.getDeadlineStatus()) && !"Completed".equalsIgnoreCase(a.getStatus()))
                .collect(Collectors.toList());

        if (pending.isEmpty()) {
            return new AdvisorDataDto(
                    "🎉 All caught up! You currently have no pending assignments.",
                    "Use your free time for self-study, exam revision, or relaxation.",
                    List.of(),
                    false
            );
        }

        AdvisorDataDto fallback = generateFallbackPrioritization(pending);

        if (apiKey.isEmpty() || "mock_key".equalsIgnoreCase(apiKey) || "your_gemini_api_key_here".equalsIgnoreCase(apiKey)) {
            fallback.setFallback(true);
            return fallback;
        }

        StringBuilder formattedAssignments = new StringBuilder();
        for (int i = 0; i < pending.size(); i++) {
            AssignmentDto a = pending.get(i);
            String overdueOrRemaining = a.getDaysRemaining() < 0
                    ? Math.abs(a.getDaysRemaining()) + " days overdue"
                    : a.getDaysRemaining() == 0 ? "DUE TODAY" : a.getDaysRemaining() + " days remaining";

            formattedAssignments.append(i + 1).append(". \"")
                    .append(a.getTitle()).append("\" (")
                    .append(a.getCourse() != null ? a.getCourse() : "Course")
                    .append(") - Due: ").append(a.getDueDate())
                    .append(" (").append(overdueOrRemaining).append(") | Deadline Status: ")
                    .append(a.getDeadlineStatus()).append(" | Urgency: ")
                    .append(a.getUrgency()).append(" | Est. Hours: ")
                    .append(a.getEstimatedHours() != null ? a.getEstimatedHours() : 2.0).append("h\n");
        }

        String examsText = (upcomingExams != null && !upcomingExams.isEmpty())
                ? String.join("\n", upcomingExams)
                : "None scheduled";

        String attendanceText = (attendanceInfo != null && !attendanceInfo.isEmpty())
                ? String.join("\n", attendanceInfo)
                : "Normal";

        String promptText = """
                You are an expert AI Academic Deadline Advisor.
                Analyze the following logged-in student's AUTHORITATIVE assignment list and academic context to provide actionable, prioritized advice.

                STRICT RULE: Only reference the exact assignments listed below. Do NOT invent missing assignments, deadlines, or exam dates.

                LOGGED-IN STUDENT PENDING ASSIGNMENTS:
                %s
                UPCOMING EXAMS:
                %s

                ATTENDANCE STANDING:
                %s

                Answer the following 5 questions concisely and structurally:
                1. What should the student focus on FIRST?
                2. WHY?
                3. How should they divide their available study time?
                4. Which assignments must be handled IMMEDIATELY?
                5. What should they avoid postponing?
                """.formatted(formattedAssignments.toString(), examsText, attendanceText);

        String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":" + escapeJsonString(promptText) + "}]}]}";

        for (String model : MODELS) {
            try {
                String uriString = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uriString))
                        .timeout(Duration.ofSeconds(5))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String extracted = extractTextFromGeminiResponse(response.body());
                    if (extracted != null && !extracted.isBlank()) {
                        return new AdvisorDataDto(
                                "AI Analysis complete for " + pending.size() + " pending assignment(s).",
                                extracted.trim(),
                                pending,
                                false
                        );
                    }
                }
            } catch (Exception e) {
                log.debug("Gemini model {} failed or timed out: {}", model, e.getMessage());
            }
        }

        fallback.setFallback(true);
        return fallback;
    }

    public AdvisorDataDto generateFallbackPrioritization(List<AssignmentDto> pending) {
        List<AssignmentDto> criticals = pending.stream()
                .filter(a -> "CRITICAL".equalsIgnoreCase(a.getUrgency()))
                .toList();

        List<AssignmentDto> highs = pending.stream()
                .filter(a -> "HIGH".equalsIgnoreCase(a.getUrgency()))
                .toList();

        String recommendation;
        if (!criticals.isEmpty()) {
            String titles = criticals.stream()
                    .map(c -> "\"" + c.getTitle() + "\"")
                    .collect(Collectors.joining(", "));
            recommendation = "🚨 **IMMEDIATE ACTION REQUIRED**: You have " + criticals.size()
                    + " CRITICAL assignment(s) (" + titles
                    + ") that are overdue or due today. Focus 100% of your current study session on completing these immediately before starting any new coursework.";
        } else if (!highs.isEmpty()) {
            AssignmentDto topHigh = highs.get(0);
            recommendation = "⚡ **HIGH PRIORITY FOCUS**: Focus on \"" + topHigh.getTitle()
                    + "\" due in " + topHigh.getDaysRemaining()
                    + " day(s). Allocate 2-3 hours today to ensure completion before the deadline.";
        } else {
            AssignmentDto top = !pending.isEmpty() ? pending.get(0) : null;
            String title = top != null ? top.getTitle() : "Coursework";
            recommendation = "✅ **STABLE WORKLOAD**: All your pending assignments are upcoming. Plan to tackle \""
                    + title + "\" early to maintain a healthy study buffer.";
        }

        return new AdvisorDataDto(
                "Deterministic Workload Analysis for " + pending.size() + " pending item(s).",
                recommendation,
                pending,
                false
        );
    }

    private static String extractTextFromGeminiResponse(String responseBody) {
        if (responseBody == null) {
            return null;
        }
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
