package com.collegecopilot.rag;

import com.collegecopilot.rag.dto.ChunkDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiRagService {

    private static final Logger log = LoggerFactory.getLogger(GeminiRagService.class);

    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final List<String> MODELS = List.of(
            "gemini-3.5-flash-lite",
            "gemini-flash-lite-latest",
            "gemini-3.5-flash",
            "gemini-3.8-flash"
    );

    private static final Set<String> STOP_WORDS = Set.of(
            "what", "when", "where", "which", "who", "whom", "whose", "why", "how",
            "does", "do", "did", "have", "has", "had", "this", "that", "these", "those",
            "about", "with", "from", "into", "during", "before", "after", "above", "below",
            "more", "most", "other", "some", "such", "only", "same", "than", "too", "very",
            "can", "will", "just", "should", "now", "the", "a", "an", "is", "are", "was", "were"
    );

    private static final Pattern TEXT_PATTERN = Pattern.compile("\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    public record GroundingVerificationResult(boolean success, String status, String answer, boolean isServiceError, String error) {}

    @Autowired
    public GeminiRagService(@Value("${GEMINI_API_KEY:${AI_API_KEY:}}") String apiKey) {
        this(apiKey, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build());
    }

    public GeminiRagService(String apiKey, HttpClient httpClient) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.httpClient = httpClient;
        this.objectMapper = JsonMapper.builder().build();
    }

    public GroundingVerificationResult answerAndVerifyDocumentQuery(String query, List<ChunkDto> docChunks) {
        if (apiKey.isEmpty() || "mock_key".equalsIgnoreCase(apiKey) || "your_gemini_api_key_here".equalsIgnoreCase(apiKey)) {
            // Local fallback logic when key is missing/mock in test/dev
            return localFallbackVerification(query, docChunks);
        }

        if (docChunks == null || docChunks.isEmpty()) {
            return new GroundingVerificationResult(true, "NOT_FOUND", "", false, null);
        }

        String formattedDocContext = formatDocumentContext(docChunks);

        String promptText = """
                You are the College Document Verification Assistant.
                Answer the user's question ONLY using the supplied official college document excerpts.

                STRICT GROUNDING & VERIFICATION RULES:
                1. If the excerpts contain enough information to directly answer the question:
                   - Provide a concise, clear, and accurate answer based ONLY on the supplied excerpts.
                   - Do NOT add outside knowledge, assumptions, or extrapolations.
                   - Do NOT invent missing details.
                   - Include relevant document title and page number citations in your answer.
                   - Set "status" to "FOUND".
                2. If the excerpts do NOT contain the answer or do not actually cover the requested topic:
                   - You MUST set "status" to "NOT_FOUND" and "answer" to "".
                   - A question MUST be considered NOT_FOUND when it merely shares general institutional, legal, administrative, or vehicular wording with the excerpts but asks about a subject that the excerpts do not cover.
                   - For example: A question about a private helicopter parking permit must be NOT_FOUND if the excerpts only discuss ordinary student motorized vehicle parking.

                %s

                User Question: %s

                OUTPUT INSTRUCTION:
                Return ONLY a valid JSON object matching this schema (do NOT wrap in markdown backticks or add extra commentary):
                {
                  "status": "FOUND" | "NOT_FOUND",
                  "answer": "<grounded answer if FOUND, or empty string if NOT_FOUND>"
                }
                """.formatted(formattedDocContext, query != null ? query.trim() : "");

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(java.util.Map.of(
                    "contents", java.util.List.of(
                            java.util.Map.of("parts", java.util.List.of(java.util.Map.of("text", promptText)))
                    )
            ));
        } catch (Exception e) {
            jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":" + escapeJson(promptText) + "}]}]}";
        }

        Exception lastError = null;

        for (String modelName : MODELS) {
            try {
                String uriString = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uriString))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    String rawText = extractTextFromGeminiResponse(response.body());
                    if (rawText != null && !rawText.isBlank()) {
                        GroundingVerificationResult parsedResult = parseGroundingResponse(rawText);
                        if (parsedResult != null) {
                            return parsedResult;
                        }
                        log.warn("Could not parse valid grounding result from Gemini model {}; trying next model or fallback", modelName);
                    }
                }
            } catch (Exception err) {
                lastError = err;
                log.debug("Gemini model {} failed in RAG grounding: {}", modelName, err.getMessage());
            }
        }

        log.warn("All live Gemini models unavailable or quota-limited for RAG grounding; engaging deterministic source fallback");
        GroundingVerificationResult fallback = localFallbackVerification(query, docChunks);
        if ("FOUND".equalsIgnoreCase(fallback.status()) && fallback.answer() != null && !fallback.answer().isBlank()) {
            String verifiedAnswer = "[Source-Verified Excerpt (AI Offline)] " + fallback.answer();
            return new GroundingVerificationResult(true, "FOUND", verifiedAnswer, false, null);
        }

        return fallback;
    }

    public String formatDocumentContext(List<ChunkDto> docChunks) {
        if (docChunks == null || docChunks.isEmpty()) return null;

        StringBuilder text = new StringBuilder("=== RELEVANT COLLEGE DOCUMENT CONTEXT ===\n");
        for (int idx = 0; idx < docChunks.size(); idx++) {
            ChunkDto c = docChunks.get(idx);
            text.append(String.format("[Excerpt %d] Source Document: \"%s\" (Page %d)\n",
                    idx + 1, c.getDocumentTitle(), c.getPageNumber()));
            text.append(String.format("Content: \"%s\"\n\n", c.getContent()));
        }
        text.append("=========================================\n");
        return text.toString();
    }

    private static final Set<String> META_WORDS = Set.of(
            "policy", "policies", "rule", "rules", "guideline", "guidelines",
            "procedure", "procedures", "process", "detail", "details", "info", "information",
            "requirement", "requirements", "criteria", "standard", "standards", "code"
    );

    private GroundingVerificationResult localFallbackVerification(String query, List<ChunkDto> docChunks) {
        if (docChunks == null || docChunks.isEmpty() || query == null || query.isBlank()) {
            return new GroundingVerificationResult(true, "NOT_FOUND", "", false, null);
        }

        String qLower = query.toLowerCase();
        List<String> terms = Arrays.stream(qLower.split("\\W+"))
                .filter(t -> t.length() > 2 && !STOP_WORDS.contains(t))
                .toList();

        if (terms.isEmpty()) {
            return new GroundingVerificationResult(true, "NOT_FOUND", "", false, null);
        }

        List<String> subjectTerms = terms.stream().filter(t -> !META_WORDS.contains(t)).toList();
        List<String> metaTerms = terms.stream().filter(META_WORDS::contains).toList();

        for (ChunkDto c : docChunks) {
            String contentLower = c.getContent().toLowerCase();
            int subjectMatches = 0;
            for (String t : subjectTerms) {
                if (contentLower.contains(t)) {
                    subjectMatches++;
                }
            }

            int metaMatches = 0;
            for (String t : metaTerms) {
                if (contentLower.contains(t)) {
                    metaMatches++;
                }
            }

            boolean isMatch;
            if (subjectTerms.isEmpty()) {
                isMatch = metaMatches >= 1;
            } else if (subjectTerms.size() == 1) {
                isMatch = subjectMatches >= 1;
            } else {
                isMatch = subjectMatches >= 2 || (subjectMatches >= 1 && metaMatches >= 1);
            }

            if (isMatch) {
                String answer = String.format("According to %s (Page %d): %s",
                        c.getDocumentTitle(), c.getPageNumber(), c.getContent().trim());
                return new GroundingVerificationResult(true, "FOUND", answer, false, null);
            }
        }

        return new GroundingVerificationResult(true, "NOT_FOUND", "", false, null);
    }

    private GroundingVerificationResult parseGroundingResponse(String rawText) {
        if (rawText == null || rawText.isBlank()) return null;

        String cleanJson = rawText.replaceAll("(?i)^```(?:json)?\\s*", "").replaceAll("```\\s*$", "").trim();

        // 1. Try ObjectMapper parsing
        try {
            JsonNode parsed = objectMapper.readTree(cleanJson);
            String status = parsed.path("status").asText("").trim().toUpperCase();
            String answer = parsed.path("answer").asText("").trim();

            if ("FOUND".equals(status) && !answer.isEmpty()) {
                return new GroundingVerificationResult(true, "FOUND", answer, false, null);
            }
            if ("NOT_FOUND".equals(status)) {
                return new GroundingVerificationResult(true, "NOT_FOUND", "", false, null);
            }
        } catch (Exception parseErr) {
            log.debug("Jackson parse of grounding JSON failed ({}), trying regex extractor: {}", parseErr.getMessage(), cleanJson);
        }

        // 2. Resilient regex extraction for status and answer
        Matcher statusMatcher = Pattern.compile("\"status\"\\s*:\\s*\"(FOUND|NOT_FOUND)\"", Pattern.CASE_INSENSITIVE).matcher(cleanJson);
        if (statusMatcher.find()) {
            String status = statusMatcher.group(1).toUpperCase();
            if ("NOT_FOUND".equals(status)) {
                return new GroundingVerificationResult(true, "NOT_FOUND", "", false, null);
            }
            Matcher answerMatcher = Pattern.compile("\"answer\"\\s*:\\s*\"([\\s\\S]*?)\"(?:\\s*\\}|\\s*,\\s*\"|$)", Pattern.DOTALL).matcher(cleanJson);
            if (answerMatcher.find()) {
                String answer = answerMatcher.group(1).trim();
                answer = answer.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");
                if (!answer.isBlank()) {
                    return new GroundingVerificationResult(true, "FOUND", answer, false, null);
                }
            }
        }

        if (cleanJson.toUpperCase().contains("\"NOT_FOUND\"") || cleanJson.equalsIgnoreCase("NOT_FOUND")) {
            return new GroundingVerificationResult(true, "NOT_FOUND", "", false, null);
        }

        return null;
    }

    private String extractTextFromGeminiResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return null;

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray()) {
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode part : parts) {
                        JsonNode textNode = part.get("text");
                        if (textNode != null && !textNode.asText().isBlank()) {
                            sb.append(textNode.asText());
                        }
                    }
                    if (sb.length() > 0) {
                        return sb.toString();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse Gemini response with ObjectMapper: {}", e.getMessage());
        }

        Matcher matcher = TEXT_PATTERN.matcher(responseBody);
        StringBuilder combined = new StringBuilder();
        while (matcher.find()) {
            combined.append(unescapeJson(matcher.group(1)));
        }
        return combined.length() > 0 ? combined.toString() : null;
    }

    private String escapeJson(String input) {
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

    private String unescapeJson(String input) {
        if (input == null) return "";
        return input
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
