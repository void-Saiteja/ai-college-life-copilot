package com.collegecopilot.attendance;

import com.collegecopilot.attendance.dto.AttendanceMetrics;
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

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final String apiKey;
    private final HttpClient httpClient;

    private static final List<String> MODELS = List.of(
            "gemini-3.5-flash-lite",
            "gemini-flash-lite-latest",
            "gemini-3.5-flash",
            "gemini-3.8-flash"
    );

    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    public GeminiService(@Value("${GEMINI_API_KEY:${AI_API_KEY:}}") String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public record ExplanationResult(String explanation, boolean isFallback) {}

    public ExplanationResult generateExplanation(String subjectCode, String subjectName, AttendanceMetrics metrics) {
        String fallback = generateFallbackExplanation(subjectName, metrics);

        if (apiKey.isEmpty() || "mock_key".equalsIgnoreCase(apiKey) || "your_gemini_api_key_here".equalsIgnoreCase(apiKey)) {
            log.warn("Gemini API key is not configured or is a placeholder. Using fallback explanation.");
            return new ExplanationResult(fallback, true);
        }

        String promptText = """
                You are an AI Academic Advisor for college students.
                Explain the following student's attendance situation using the exact calculated backend numbers provided.

                ATTENDANCE DATA:
                - Subject: %s - %s
                - Classes Conducted: %d
                - Classes Attended: %d
                - Current Attendance: %.2f%%
                - Required Attendance Target: %d%%
                - Maximum Classes Still Allowed to Miss: %d
                - Minimum Consecutive Classes Required to Attend: %d
                - Risk Level: %s

                Provide a concise (3-4 sentences), encouraging, and clear explanation of their situation and actionable advice on what they must do next.
                """.formatted(
                subjectCode != null && !subjectCode.isBlank() ? subjectCode : "CS",
                subjectName != null && !subjectName.isBlank() ? subjectName : "Subject",
                metrics.getConducted(),
                metrics.getAttended(),
                metrics.getPercentage(),
                metrics.getTargetPercentage(),
                metrics.getMaxCanMiss(),
                metrics.getMinRequiredToAttend(),
                metrics.getRiskClassification()
        );

        String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":" + escapeJsonString(promptText) + "}]}]}";

        for (String model : MODELS) {
            try {
                String uriString = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uriString))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String extracted = extractTextFromGeminiResponse(response.body());
                    if (extracted != null && !extracted.isBlank()) {
                        return new ExplanationResult(extracted, false);
                    }
                    log.warn("Gemini model {} returned status {} but extracted text was blank", model, response.statusCode());
                } else {
                    String sanitized = sanitizeErrorBody(response.body());
                    log.warn("Gemini model {} call returned HTTP status {}: {}", model, response.statusCode(), sanitized);
                }
            } catch (Exception e) {
                log.warn("Gemini model {} request failed: {} - {}", model, e.getClass().getSimpleName(), e.getMessage());
            }
        }

        return new ExplanationResult(fallback, true);
    }

    public String generateFallbackExplanation(String subjectName, AttendanceMetrics metrics) {
        String name = (subjectName != null && !subjectName.isBlank()) ? subjectName : "Subject";
        String risk = metrics.getRiskClassification();
        if ("CRITICAL".equals(risk)) {
            return "Your attendance in " + name + " is currently at " + metrics.getPercentage()
                    + "%, which is below the required " + metrics.getTargetPercentage()
                    + "% threshold. You must attend the next " + metrics.getMinRequiredToAttend()
                    + " consecutive classes without missing any to restore exam eligibility.";
        } else if ("WARNING".equals(risk)) {
            return "Your attendance in " + name + " is at " + metrics.getPercentage()
                    + "%, meeting the minimum " + metrics.getTargetPercentage()
                    + "% target. However, your margin is slim—you can only afford to miss " + metrics.getMaxCanMiss()
                    + " more class(es) before falling into the critical zone.";
        } else if ("NO_CLASSES".equals(risk)) {
            return "No classes have been conducted yet for " + name
                    + ". Maintain regular attendance once classes start to stay above "
                    + metrics.getTargetPercentage() + "%.";
        } else {
            return "Great job! Your attendance in " + name + " is at a strong " + metrics.getPercentage()
                    + "%, well above the " + metrics.getTargetPercentage()
                    + "% requirement. You have a safety buffer of " + metrics.getMaxCanMiss()
                    + " class(es) you could miss if needed.";
        }
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

    private static String sanitizeErrorBody(String body) {
        if (body == null || body.isBlank()) {
            return "empty response";
        }
        String clean = body.replaceAll("(?i)(key|token|password)=[^&\"\\s]+", "$1=[REDACTED]")
                           .replaceAll("AIza[0-9A-Za-z-_]{35,}", "[REDACTED]");
        clean = clean.replace('\n', ' ').replace('\r', ' ').trim();
        if (clean.length() > 250) {
            return clean.substring(0, 250) + "... (truncated)";
        }
        return clean;
    }
}
