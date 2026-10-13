package com.collegecopilot.note;

import com.collegecopilot.note.dto.FlashcardDto;
import com.collegecopilot.note.dto.NoteSummaryDataDto;
import com.collegecopilot.note.dto.QuizItemDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class GeminiNoteSummarizerService {

    private static final Logger log = LoggerFactory.getLogger(GeminiNoteSummarizerService.class);

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

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

    @Autowired
    public GeminiNoteSummarizerService(@Value("${GEMINI_API_KEY:${AI_API_KEY:}}") String apiKey) {
        this(apiKey, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build());
    }

    public GeminiNoteSummarizerService(String apiKey, HttpClient httpClient) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.httpClient = httpClient;
        this.objectMapper = JsonMapper.builder().build();
    }

    public boolean detectPromptInjection(String text) {
        if (text == null || text.isBlank()) return false;
        String lower = text.toLowerCase();
        return DANGEROUS_PATTERNS.stream().anyMatch(lower::contains);
    }

    public NoteSummaryDataDto generateSummary(String title, String course, String content) {
        String safeTitle = title != null && !title.isBlank() ? title.trim() : "AI Generated Note Summary";
        String safeCourse = course != null && !course.isBlank() ? course.trim() : "General Study";

        // Prompt Injection Defense
        if (detectPromptInjection(content) || detectPromptInjection(title)) {
            return generateSecurityNotice(safeTitle, safeCourse);
        }

        // If Gemini API Key is configured, attempt LLM generation first
        if (!apiKey.isBlank()) {
            try {
                NoteSummaryDataDto llmResult = callGeminiSummarizer(safeTitle, safeCourse, content);
                if (llmResult != null) {
                    return llmResult;
                }
            } catch (Exception e) {
                log.warn("Gemini summarizer call failed, using deterministic NLP fallback: {}", e.getMessage());
            }
        }

        // Deterministic rule-based / NLP generator
        return generateRuleBasedSummary(safeTitle, safeCourse, content);
    }

    public NoteSummaryDataDto generateSecurityNotice(String title, String course) {
        String cleanTitle = (title != null && !title.isBlank())
                ? title.substring(0, Math.min(100, title.length()))
                : "Security Notice";

        return new NoteSummaryDataDto(
                cleanTitle,
                (course != null && !course.isBlank()) ? course : "Academic Ethics",
                15,
                1,
                "Institutional Security Protocol: The system prompt, internal application secrets, and peer student records are protected and cannot be disclosed.",
                List.of(
                        "• Adheres to FERPA and academic data privacy standards.",
                        "• Security instructions and configuration parameters are confidential.",
                        "• External instruction overrides are neutralized."
                ),
                List.of(
                        new FlashcardDto(
                                "What is data confidentiality in academic applications?",
                                "Ensuring student records are segregated and access is restricted exclusively to the authenticated owner."
                        )
                ),
                List.of(
                        new QuizItemDto(
                                1,
                                "Are student records shared across user accounts?",
                                List.of("Never - strictly segregated", "Always shared", "Publicly accessible", "Unprotected"),
                                0,
                                "Enforced via role-based access control and IDOR protections."
                        )
                )
        );
    }

    public NoteSummaryDataDto generateRuleBasedSummary(String title, String course, String content) {
        String[] rawSentences = content != null ? content.split("(?<=[.?!])\\s+") : new String[0];
        List<String> sentences = Arrays.stream(rawSentences)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        int wordCount = content != null ? content.trim().split("\\s+").length : 0;
        int readingTime = Math.max(1, (int) Math.ceil((double) wordCount / 180.0));

        // Key takeaways: up to 4 sentences
        List<String> keyTakeaways = new ArrayList<>();
        int takeawayCount = Math.min(4, sentences.size());
        for (int i = 0; i < takeawayCount; i++) {
            keyTakeaways.add("• " + sentences.get(i));
        }
        if (keyTakeaways.isEmpty()) {
            keyTakeaways.add("• Foundational concepts outlined in the study notes.");
        }

        String s0 = !sentences.isEmpty() ? sentences.get(0) : "Core foundational concept from the study notes.";
        String s1 = sentences.size() > 1 ? sentences.get(1) : "It provides the underlying principles needed to solve advanced problems.";
        String s2 = sentences.size() > 2 ? sentences.get(2) : "Applying structured methods to streamline computation and accuracy.";

        // Flashcards
        List<FlashcardDto> flashcards = List.of(
                new FlashcardDto("What is the core definition presented in \"" + title + "\"?", s0),
                new FlashcardDto("Why is this important for " + course + "?", s1),
                new FlashcardDto("What is a key application or operation mentioned?", s2)
        );

        // Quiz
        String s0Snippet = s0.length() > 50 ? s0.substring(0, 50) : s0;
        String s1Snippet = s1.length() > 50 ? s1.substring(0, 50) : s1;

        List<QuizItemDto> quiz = List.of(
                new QuizItemDto(
                        1,
                        "Which key concept is most central to " + title + "?",
                        List.of(s0Snippet, "Random unrelated formula", "Deprecated legacy method", "External environmental factor"),
                        0,
                        "Directly stated in the primary summary definition."
                ),
                new QuizItemDto(
                        2,
                        "What is the main benefit or requirement highlighted?",
                        List.of("High memory overhead", s1Snippet, "Manual error check requirement", "None of the above"),
                        1,
                        "Extracted from key takeaway notes."
                )
        );

        String summarySentences = String.join(" ", sentences.subList(0, Math.min(2, sentences.size())));
        String summary = "This note covers essential topics for " + course + ", focusing on " + summarySentences;

        return new NoteSummaryDataDto(
                title,
                course,
                wordCount,
                readingTime,
                summary,
                keyTakeaways,
                flashcards,
                quiz
        );
    }

    private NoteSummaryDataDto callGeminiSummarizer(String title, String course, String content) throws Exception {
        String truncatedContent = content.length() > 6000 ? content.substring(0, 6000) : content;

        String prompt = """
                Analyze these student lecture notes and produce a valid JSON study guide.
                Course: %s
                Title: %s
                Notes Content:
                %s

                Respond with strictly a single JSON object containing:
                {
                  "summary": "comprehensive 2-3 sentence overview",
                  "keyTakeaways": ["• point 1", "• point 2", "• point 3"],
                  "flashcards": [
                    {"question": "Q1", "answer": "A1"},
                    {"question": "Q2", "answer": "A2"}
                  ],
                  "quiz": [
                    {
                      "id": 1,
                      "question": "Question text",
                      "options": ["Option A", "Option B", "Option C", "Option D"],
                      "correctAnswer": 0,
                      "explanation": "Why option A is correct"
                    }
                  ]
                }
                Do not include markdown code block backticks.
                """.formatted(course, title, truncatedContent);

        String jsonPayload = objectMapper.writeValueAsString(java.util.Map.of(
                "contents", List.of(
                        java.util.Map.of("parts", List.of(java.util.Map.of("text", prompt)))
                )
        ));

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(6))
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return null;
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode candidate = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (candidate.isMissingNode() || candidate.asText().isBlank()) {
            return null;
        }

        String rawText = candidate.asText().trim();
        if (rawText.startsWith("```json")) rawText = rawText.substring(7);
        if (rawText.startsWith("```")) rawText = rawText.substring(3);
        if (rawText.endsWith("```")) rawText = rawText.substring(0, rawText.length() - 3);
        rawText = rawText.trim();

        JsonNode parsed = objectMapper.readTree(rawText);
        String summary = parsed.path("summary").asText();
        if (summary.isBlank()) return null;

        List<String> keyTakeaways = new ArrayList<>();
        parsed.path("keyTakeaways").forEach(node -> keyTakeaways.add(node.asText()));

        List<FlashcardDto> flashcards = new ArrayList<>();
        parsed.path("flashcards").forEach(node -> flashcards.add(new FlashcardDto(
                node.path("question").asText(),
                node.path("answer").asText()
        )));

        List<QuizItemDto> quiz = new ArrayList<>();
        parsed.path("quiz").forEach(node -> {
            List<String> opts = new ArrayList<>();
            node.path("options").forEach(opt -> opts.add(opt.asText()));
            quiz.add(new QuizItemDto(
                    node.path("id").asInt(1),
                    node.path("question").asText(),
                    opts,
                    node.path("correctAnswer").asInt(0),
                    node.path("explanation").asText()
            ));
        });

        int words = content.trim().split("\\s+").length;
        int readingTime = Math.max(1, (int) Math.ceil((double) words / 180.0));

        return new NoteSummaryDataDto(
                title,
                course,
                words,
                readingTime,
                summary,
                keyTakeaways,
                flashcards,
                quiz
        );
    }
}
