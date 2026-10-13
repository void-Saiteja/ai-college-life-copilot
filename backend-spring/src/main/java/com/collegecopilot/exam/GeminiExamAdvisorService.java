package com.collegecopilot.exam;

import com.collegecopilot.exam.dto.ExamAdvisorDataDto;
import com.collegecopilot.exam.dto.ExamDto;
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
public class GeminiExamAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(GeminiExamAdvisorService.class);

    private final String apiKey;
    private final HttpClient httpClient;

    private static final List<String> MODELS = List.of(
            "gemini-1.5-flash",
            "gemini-2.0-flash",
            "gemini-2.5-flash"
    );

    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    public GeminiExamAdvisorService(@Value("${GEMINI_API_KEY:${AI_API_KEY:}}") String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public ExamAdvisorDataDto generatePrioritization(List<ExamDto> exams, List<String> pendingAssignments, List<String> attendanceSummary) {
        List<ExamDto> upcomingExams = exams.stream()
                .filter(e -> !"COMPLETED".equalsIgnoreCase(e.getExamStatus()) && !"Completed".equalsIgnoreCase(e.getStatus()))
                .collect(Collectors.toList());

        if (upcomingExams.isEmpty()) {
            return new ExamAdvisorDataDto(
                    "🎉 No upcoming exams scheduled on your calendar.",
                    "You currently have no pending exams. Maintain consistent class attendance and coursework revision.",
                    List.of(),
                    false
            );
        }

        ExamAdvisorDataDto fallback = generateFallbackExamAdvice(upcomingExams);

        if (apiKey.isEmpty() || "mock_key".equalsIgnoreCase(apiKey) || "your_gemini_api_key_here".equalsIgnoreCase(apiKey)) {
            fallback.setFallback(true);
            return fallback;
        }

        StringBuilder formattedExams = new StringBuilder();
        for (int i = 0; i < upcomingExams.size(); i++) {
            ExamDto e = upcomingExams.get(i);
            String dayStr = e.getDaysRemaining() == 0 ? "TODAY" : "in " + e.getDaysRemaining() + " days";
            formattedExams.append(i + 1).append(". \"")
                    .append(e.getTitle()).append("\" (")
                    .append(e.getSubjectName() != null ? e.getSubjectName() : e.getSubjectCode()).append(") - Date: ")
                    .append(e.getExamDate()).append(" (").append(dayStr).append(") | Status: ")
                    .append(e.getExamStatus()).append(" | Urgency: ")
                    .append(e.getUrgency()).append(" | Location: ")
                    .append(e.getLocation() != null ? e.getLocation() : "Main Hall").append(" | Syllabus: ")
                    .append(e.getSyllabus() != null ? e.getSyllabus() : "Full syllabus").append("\n");
        }

        String assignmentsText = (pendingAssignments != null && !pendingAssignments.isEmpty())
                ? String.join("\n", pendingAssignments)
                : "No pending assignments";

        String attendanceText = (attendanceSummary != null && !attendanceSummary.isEmpty())
                ? String.join("\n", attendanceSummary)
                : "All attendance normal";

        String promptText = """
                You are an expert AI Exam & Academic Advisor.
                Analyze the following logged-in student's AUTHORITATIVE exam schedule, assignments, and attendance data to provide strategic, prioritized guidance.

                STRICT RULE: Only reference the exact exams, dates, assignments, and attendance metrics provided below. Do NOT invent missing exams, subjects, or deadlines.

                AUTHORITATIVE UPCOMING EXAMS:
                %s
                RELATED PENDING ASSIGNMENTS:
                %s

                ATTENDANCE STANDING:
                %s

                Answer the following questions concisely and structurally:
                1. Which exam should the student focus on FIRST?
                2. How should they divide their daily study time between exam review and pending assignments?
                3. What specific topics/syllabus items should they study TODAY?
                4. Which assignments MUST be completed before the upcoming exams?
                5. How should attendance risks affect their daily study priorities?
                6. Strategic advice for managing multiple approaching exams.
                """.formatted(formattedExams.toString(), assignmentsText, attendanceText);

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
                        return new ExamAdvisorDataDto(
                                "AI Exam Analysis complete for " + upcomingExams.size() + " upcoming exam(s).",
                                extracted.trim(),
                                upcomingExams,
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

    public ExamAdvisorDataDto generateFallbackExamAdvice(List<ExamDto> upcomingExams) {
        List<ExamDto> criticalExams = upcomingExams.stream()
                .filter(e -> "CRITICAL".equalsIgnoreCase(e.getUrgency()))
                .toList();

        List<ExamDto> highExams = upcomingExams.stream()
                .filter(e -> "HIGH".equalsIgnoreCase(e.getUrgency()))
                .toList();

        String recommendation;
        if (!criticalExams.isEmpty()) {
            recommendation = "🚨 **CRITICAL EXAM TODAY**: You have " + criticalExams.size()
                    + " exam(s) scheduled TODAY (\"" + criticalExams.get(0).getTitle()
                    + "\"). Review summary formulas and key concepts immediately. Focus all study energy on this exam.";
        } else if (!highExams.isEmpty()) {
            ExamDto topHigh = highExams.get(0);
            recommendation = "⚡ **HIGH EXAM PRIORITY**: Focus on \"" + topHigh.getTitle()
                    + "\" taking place in " + topHigh.getDaysRemaining()
                    + " day(s). Dedicate 70% of your daily study schedule to active recall and problem-solving for this syllabus: "
                    + (topHigh.getSyllabus() != null && !topHigh.getSyllabus().isBlank() ? topHigh.getSyllabus() : "Core topics") + ".";
        } else {
            ExamDto next = upcomingExams.get(0);
            recommendation = "📘 **UPCOMING EXAM REVISION**: Next exam is \"" + next.getTitle()
                    + "\" in " + next.getDaysRemaining()
                    + " days. Create 45-minute daily study blocks for key topics to build a robust preparation buffer.";
        }

        return new ExamAdvisorDataDto(
                "Deterministic Exam Strategy for " + upcomingExams.size() + " upcoming exam(s).",
                recommendation,
                upcomingExams,
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
