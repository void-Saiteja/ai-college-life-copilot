package com.collegecopilot.quiz;

import com.collegecopilot.quiz.dto.QuizAdvisorDataDto;
import com.collegecopilot.quiz.dto.QuizQuestionDto;
import com.collegecopilot.student.QuizAttempt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
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
public class GeminiQuizService {

    private static final Logger log = LoggerFactory.getLogger(GeminiQuizService.class);

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final List<String> MODELS = List.of(
            "gemini-1.5-flash",
            "gemini-2.0-flash",
            "gemini-2.5-flash"
    );

    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    public record QuizGenerationResult(List<QuizQuestionDto> questions, boolean isFallback) {}
    public record AdvisorAttempt(String subject, String topic, double percentage, int score, int totalQuestions) {}

    public GeminiQuizService(@Value("${GEMINI_API_KEY:${AI_API_KEY:}}") String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public boolean validateQuizQuestion(QuizQuestionDto q) {
        if (q == null) return false;
        if (q.getQuestion() == null || q.getQuestion().trim().isEmpty()) return false;
        if (q.getOptions() == null || q.getOptions().size() < 2) return false;
        Integer answerIdx = q.getCorrectAnswer();
        if (answerIdx == null || answerIdx < 0 || answerIdx >= q.getOptions().size()) return false;
        return true;
    }

    public QuizGenerationResult generateQuizQuestions(String subject, String topic, String difficulty, int count, List<String> upcomingExams) {
        int targetCount = Math.max(1, Math.min(10, count));

        if (apiKey.isEmpty() || "mock_key".equalsIgnoreCase(apiKey) || "your_gemini_api_key_here".equalsIgnoreCase(apiKey)) {
            return new QuizGenerationResult(generateFallbackQuestions(subject, topic, targetCount), true);
        }

        String examsText = (upcomingExams != null && !upcomingExams.isEmpty())
                ? String.join("\n", upcomingExams)
                : "None";

        String promptText = """
                You are an expert AI Exam & Quiz Generator for college students.
                Generate a high-quality, practical multiple-choice quiz (MCQ) for exam preparation.

                QUIZ CONFIGURATION:
                - Subject: %s
                - Topic/Unit: %s
                - Difficulty Level: %s
                - Number of Questions: %d

                LOGGED-IN STUDENT CONTEXT:
                - Upcoming Exams: %s

                STRICT JSON OUTPUT REQUIREMENT:
                Return ONLY a raw valid JSON array (no markdown block, no conversational text) containing exactly %d question objects:
                [
                  {
                    "id": 1,
                    "question": "Clear and unambiguous multiple choice question text?",
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "correctAnswer": 0,
                    "explanation": "Detailed explanation of why Option A is correct and why other options are incorrect."
                  }
                ]
                """.formatted(subject, topic, difficulty, targetCount, examsText, targetCount);

        String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":" + escapeJsonString(promptText) + "}]}]}";

        for (String model : MODELS) {
            try {
                String uriString = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uriString))
                        .timeout(Duration.ofSeconds(8))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String extracted = extractTextFromGeminiResponse(response.body());
                    if (extracted != null && !extracted.isBlank()) {
                        String cleanJson = extracted
                                .replaceAll("(?i)```json", "")
                                .replaceAll("```", "")
                                .trim();

                        List<QuizQuestionDto> parsed = objectMapper.readValue(cleanJson, new TypeReference<List<QuizQuestionDto>>() {});
                        if (parsed != null && !parsed.isEmpty()) {
                            List<QuizQuestionDto> validQuestions = new ArrayList<>();
                            for (int i = 0; i < parsed.size(); i++) {
                                QuizQuestionDto q = parsed.get(i);
                                if (validateQuizQuestion(q)) {
                                    validQuestions.add(new QuizQuestionDto(
                                            i + 1,
                                            q.getQuestion().trim(),
                                            q.getOptions().stream().map(String::trim).collect(Collectors.toList()),
                                            q.getCorrectAnswer(),
                                            q.getExplanation() != null ? q.getExplanation().trim() : "Correct answer verified."
                                    ));
                                }
                            }

                            if (!validQuestions.isEmpty()) {
                                return new QuizGenerationResult(validQuestions, false);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Gemini model {} failed during quiz generation: {}", model, e.getMessage());
            }
        }

        return new QuizGenerationResult(generateFallbackQuestions(subject, topic, targetCount), true);
    }

    public QuizAdvisorDataDto generateAdvisorAdvice(List<AdvisorAttempt> attempts, List<String> upcomingExams) {
        if (attempts == null || attempts.isEmpty()) {
            return new QuizAdvisorDataDto(
                    "No quiz performance history found.",
                    "You have not taken any quizzes yet. Generate a topic quiz for your upcoming exams to establish a revision baseline.",
                    List.of(),
                    List.of(),
                    false
            );
        }

        // Calculate weak and strong topics deterministically
        Map<String, List<Double>> topicScores = new HashMap<>();
        for (AdvisorAttempt a : attempts) {
            String topicKey = (a.subject() != null ? a.subject() : "CS") + " - " + (a.topic() != null ? a.topic() : "General");
            double pct = a.percentage();
            topicScores.computeIfAbsent(topicKey, k -> new ArrayList<>()).add(pct);
        }

        List<WeakTopicInfo> weakTopics = new ArrayList<>();
        List<WeakTopicInfo> strongTopics = new ArrayList<>();

        topicScores.forEach((t, scores) -> {
            double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double rounded = Math.round(avg * 10.0) / 10.0;
            if (avg < 60) {
                weakTopics.add(new WeakTopicInfo(t, rounded));
            } else if (avg >= 80) {
                strongTopics.add(new WeakTopicInfo(t, rounded));
            }
        });

        QuizAdvisorDataDto fallback = generateFallbackAdvisorAdvice(attempts, weakTopics, strongTopics);

        if (apiKey.isEmpty() || "mock_key".equalsIgnoreCase(apiKey) || "your_gemini_api_key_here".equalsIgnoreCase(apiKey)) {
            fallback.setFallback(true);
            return fallback;
        }

        StringBuilder formattedAttempts = new StringBuilder();
        int limit = Math.min(5, attempts.size());
        for (int i = 0; i < limit; i++) {
            AdvisorAttempt a = attempts.get(i);
            formattedAttempts.append(String.format("- %s (%s): %.1f%% (%d/%d)\n",
                    a.subject() != null ? a.subject() : "CS",
                    a.topic() != null ? a.topic() : "Topic",
                    a.percentage(),
                    a.score(),
                    a.totalQuestions()));
        }

        String examsText = (upcomingExams != null && !upcomingExams.isEmpty())
                ? String.join("\n", upcomingExams)
                : "None";

        StringBuilder weakText = new StringBuilder();
        if (weakTopics.isEmpty()) {
            weakText.append("None identified (<60%)");
        } else {
            for (WeakTopicInfo w : weakTopics) {
                weakText.append(String.format("- %s (Avg: %.1f%%)\n", w.topic(), w.avgScore()));
            }
        }

        StringBuilder strongText = new StringBuilder();
        if (strongTopics.isEmpty()) {
            strongText.append("None");
        } else {
            for (WeakTopicInfo s : strongTopics) {
                strongText.append(String.format("- %s (Avg: %.1f%%)\n", s.topic(), s.avgScore()));
            }
        }

        String promptText = """
                You are an expert AI Quiz & Revision Advisor for college students.
                Analyze the student's AUTHORITATIVE quiz attempt history and upcoming exam schedule.

                STRICT RULE: Only reference actual quiz attempts and exams listed below. Do NOT invent fake scores or missing tests.

                QUIZ ATTEMPT HISTORY:
                %s
                DETERMINISTIC WEAK TOPICS (<60%%):
                %s
                DETERMINISTIC STRONG TOPICS (>=80%%):
                %s
                UPCOMING EXAMS:
                %s

                Answer the following questions concisely:
                1. Which topic should the student revise FIRST?
                2. Why are they weak in this topic based on quiz performance?
                3. What specific practice questions or concepts should they tackle next?
                4. How should they align their quiz revision with upcoming exam deadlines?
                """.formatted(formattedAttempts.toString(), weakText.toString(), strongText.toString(), examsText);

        String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":" + escapeJsonString(promptText) + "}]}]}";

        for (String model : MODELS) {
            try {
                String uriString = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uriString))
                        .timeout(Duration.ofSeconds(6))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String extracted = extractTextFromGeminiResponse(response.body());
                    if (extracted != null && !extracted.isBlank()) {
                        return new QuizAdvisorDataDto(
                                String.format("AI Revision Strategy for %d quiz attempt(s).", attempts.size()),
                                extracted.trim(),
                                weakTopics.stream().map(WeakTopicInfo::topic).collect(Collectors.toList()),
                                strongTopics.stream().map(WeakTopicInfo::topic).collect(Collectors.toList()),
                                false
                        );
                    }
                }
            } catch (Exception e) {
                log.debug("Gemini model {} failed during quiz advisor: {}", model, e.getMessage());
            }
        }

        fallback.setFallback(true);
        return fallback;
    }

    public List<QuizQuestionDto> generateFallbackQuestions(String subject, String topic, int count) {
        String safeTopic = (topic != null && !topic.isBlank()) ? topic : "Data Structures";
        String safeSubject = (subject != null && !subject.isBlank()) ? subject : "Computer Science";

        List<QuizQuestionDto> sample = List.of(
                new QuizQuestionDto(
                        1,
                        String.format("In %s, what is the primary purpose of time complexity analysis?", safeTopic),
                        List.of("To measure exact execution time in milliseconds", "To evaluate algorithm efficiency as input size grows", "To count lines of code", "To verify compiler version"),
                        1,
                        "Time complexity analysis uses Asymptotic Notation (Big-O) to evaluate how resource consumption grows relative to input size N."
                ),
                new QuizQuestionDto(
                        2,
                        String.format("Which data structure operating principle applies to %s?", safeTopic),
                        List.of("First In First Out (FIFO)", "Last In First Out (LIFO)", "Random Access Memory", "Asymmetric Key Cryptography"),
                        0,
                        "Standard queues and buffer streams follow First In First Out (FIFO) ordering."
                ),
                new QuizQuestionDto(
                        3,
                        String.format("What is the worst-case space complexity of storing N nodes in a binary tree in %s?", safeTopic),
                        List.of("O(1)", "O(log N)", "O(N)", "O(N^2)"),
                        2,
                        "Storing N nodes requires memory proportional to N, resulting in O(N) space complexity."
                ),
                new QuizQuestionDto(
                        4,
                        String.format("Which strategy is most effective when preparing for an exam in %s?", safeSubject),
                        List.of("Passive re-reading of notes", "Active recall and timed MCQ practice", "Cramming 1 hour before exam", "Memorizing code without execution"),
                        1,
                        "Active recall and timed self-testing produce significantly higher long-term retention and problem-solving readiness."
                ),
                new QuizQuestionDto(
                        5,
                        "In computer science theory, what does a balanced search tree guarantee for search operations?",
                        List.of("O(1) time complexity", "O(log N) time complexity", "O(N) time complexity", "O(N log N) time complexity"),
                        1,
                        "Balanced search trees (like AVL or Red-Black trees) guarantee logarithmic O(log N) height, ensuring O(log N) search time."
                ),
                new QuizQuestionDto(
                        6,
                        String.format("In systems and %s, what problem does a Semaphore primarily solve?", safeTopic),
                        List.of("Race conditions and mutual exclusion", "Static code compilation", "Dynamic web routing", "Database schema migration"),
                        0,
                        "Semaphores provide synchronization primitives to control access to shared critical sections, preventing race conditions."
                ),
                new QuizQuestionDto(
                        7,
                        "In relational database management, what does Atomicity in ACID transactions guarantee?",
                        List.of("Asynchronous network replication", "All-or-nothing execution of transaction operations", "Automatic cloud clustering", "Associative query caching"),
                        1,
                        "Atomicity guarantees that either all operations within a database transaction complete successfully, or all changes are rolled back."
                ),
                new QuizQuestionDto(
                        8,
                        "At the transport layer of computer networks, what distinguishes TCP from UDP?",
                        List.of("TCP is connection-oriented and reliable, while UDP is connectionless", "TCP has lower packet overhead than UDP", "UDP provides guaranteed in-order packet delivery", "TCP does not utilize port addressing"),
                        0,
                        "TCP establishes a connection with acknowledgments for reliable transmission, whereas UDP is connectionless and optimized for low-latency streaming."
                ),
                new QuizQuestionDto(
                        9,
                        String.format("Which graph traversal technique systematically uses a FIFO queue to visit neighboring nodes in %s?", safeTopic),
                        List.of("Depth-First Search (DFS)", "Breadth-First Search (BFS)", "Binary Search", "Quicksort Partitioning"),
                        1,
                        "Breadth-First Search (BFS) uses a FIFO queue to traverse graphs layer-by-layer from the starting vertex."
                ),
                new QuizQuestionDto(
                        10,
                        "In modern software design, what is the primary architectural benefit of Dependency Injection?",
                        List.of("Eliminates CPU branch misprediction", "Decouples components to improve testability and maintainability", "Compiles code directly into native machine instructions", "Bypasses all network firewalls"),
                        1,
                        "Dependency Injection decouples object creation from business logic, making classes loosely coupled and easy to unit test with mocks."
                )
        );

        int limit = Math.min(count, sample.size());
        return new ArrayList<>(sample.subList(0, limit));
    }

    private QuizAdvisorDataDto generateFallbackAdvisorAdvice(List<AdvisorAttempt> attempts, List<WeakTopicInfo> weakTopics, List<WeakTopicInfo> strongTopics) {
        String recommendation;
        if (!weakTopics.isEmpty()) {
            WeakTopicInfo top = weakTopics.get(0);
            recommendation = String.format("🎯 **REVISION FOCUS**: Your quiz results indicate weak mastery (<60%%) in **%s** (Avg: %.1f%%). We recommend generating a targeted 5-question quiz for this topic before your upcoming exams.",
                    top.topic(), top.avgScore());
        } else if (!strongTopics.isEmpty()) {
            WeakTopicInfo top = strongTopics.get(0);
            recommendation = String.format("🌟 **STRONG MASTERY**: You demonstrate solid performance (≥80%%) in **%s**! Focus your upcoming study sessions on remaining core subjects.",
                    top.topic());
        } else {
            recommendation = String.format("📚 **BALANCED PROGRESS**: Your average quiz score across %d attempts is moderate. Take additional quizzes for approaching exam subjects to build high confidence.",
                    attempts.size());
        }

        return new QuizAdvisorDataDto(
                String.format("Deterministic Performance Analysis for %d attempt(s).", attempts.size()),
                recommendation,
                weakTopics.stream().map(WeakTopicInfo::topic).collect(Collectors.toList()),
                strongTopics.stream().map(WeakTopicInfo::topic).collect(Collectors.toList()),
                true
        );
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

    private record WeakTopicInfo(String topic, double avgScore) {}
}
