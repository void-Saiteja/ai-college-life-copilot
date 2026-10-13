package com.collegecopilot.rag;

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
import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingService.class);

    private final String apiKey;
    private final String embeddingModel;
    private final int embeddingDimension;
    private final String embeddingVersion;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public record EmbeddingResult(double[] values, String provider, String model, int dimension, String version) {}

    @Autowired
    public GeminiEmbeddingService(@Value("${GEMINI_API_KEY:${AI_API_KEY:}}") String apiKey,
                                  @Value("${gemini.embedding-model:gemini-embedding-2}") String embeddingModel,
                                  @Value("${gemini.embedding-dimension:768}") int embeddingDimension,
                                  @Value("${gemini.embedding-version:v1}") String embeddingVersion) {
        this(apiKey, embeddingModel, embeddingDimension, embeddingVersion,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build());
    }

    public GeminiEmbeddingService(String apiKey, String embeddingModel, int embeddingDimension,
                                  String embeddingVersion, HttpClient httpClient) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.embeddingModel = (embeddingModel != null && !embeddingModel.isBlank()) ? embeddingModel : "gemini-embedding-2";
        this.embeddingDimension = embeddingDimension > 0 ? embeddingDimension : 768;
        this.embeddingVersion = (embeddingVersion != null && !embeddingVersion.isBlank()) ? embeddingVersion : "v1";
        this.httpClient = httpClient;
        this.objectMapper = JsonMapper.builder().build();
    }

    public EmbeddingResult generateSemanticEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new EmbeddingResult(new double[embeddingDimension], "google", embeddingModel, embeddingDimension, embeddingVersion);
        }

        boolean hasValidKey = !apiKey.isEmpty() && !"mock_key".equalsIgnoreCase(apiKey) && !"your_gemini_api_key_here".equalsIgnoreCase(apiKey);

        if (hasValidKey) {
            try {
                String uriString = "https://generativelanguage.googleapis.com/v1beta/models/" + embeddingModel + ":embedContent?key=" + apiKey;
                String jsonPayload = String.format(
                        "{\"model\":\"models/%s\",\"content\":{\"parts\":[{\"text\":%s}]},\"outputDimensionality\":%d}",
                        embeddingModel,
                        escapeJson(text.trim()),
                        embeddingDimension
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(uriString))
                        .timeout(Duration.ofSeconds(8))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    JsonNode root = objectMapper.readTree(response.body());
                    JsonNode valuesNode = root.path("embedding").path("values");
                    if (valuesNode.isMissingNode() || !valuesNode.isArray()) {
                        valuesNode = root.path("embeddings").get(0).path("values");
                    }

                    if (valuesNode.isArray() && valuesNode.size() > 0) {
                        double[] vector = new double[valuesNode.size()];
                        for (int i = 0; i < valuesNode.size(); i++) {
                            double val = valuesNode.get(i).asDouble();
                            if (!Double.isFinite(val)) {
                                throw new IllegalStateException("Embedding vector contains non-finite numerical values");
                            }
                            vector[i] = val;
                        }

                        if (vector.length != embeddingDimension) {
                            log.warn("Embedding dimension mismatch: expected {}, received {}", embeddingDimension, vector.length);
                        }

                        return new EmbeddingResult(vector, "google", embeddingModel, vector.length, embeddingVersion);
                    }
                }
                log.warn("Gemini Embedding API returned non-200 code: {}", response.statusCode());
            } catch (Exception e) {
                log.error("Gemini Embedding API call failed: {}", e.getMessage());
            }
        }

        // Deterministic local feature vector for test and development environments
        double[] localVector = computeLocalSemanticVector(text.trim(), embeddingDimension);
        return new EmbeddingResult(localVector, "local-fallback", embeddingModel, embeddingDimension, embeddingVersion);
    }

    public double cosineSimilarity(double[] vecA, double[] vecB) {
        if (vecA == null || vecB == null || vecA.length == 0 || vecB.length == 0) {
            return 0.0;
        }

        if (vecA.length != vecB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private double[] computeLocalSemanticVector(String text, int dim) {
        double[] vector = new double[dim];
        if (text == null || text.isBlank()) return vector;

        String[] words = text.toLowerCase().split("\\W+");
        for (String w : words) {
            if (w.isBlank()) continue;
            int hash = Math.abs(w.hashCode());
            int idx = Math.floorMod(hash, dim);
            vector[idx] += 1.0;
            // Also spread secondary hash safely
            int idx2 = (int) Math.floorMod((long) hash * 31L, (long) dim);
            vector[idx2] += 0.5;
        }

        // Normalize
        double norm = 0.0;
        for (double v : vector) norm += v * v;
        if (norm > 0) {
            double sqrtNorm = Math.sqrt(norm);
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= sqrtNorm;
            }
        }

        return vector;
    }

    private String escapeJson(String text) {
        if (text == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
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

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    public String getEmbeddingVersion() {
        return embeddingVersion;
    }
}
