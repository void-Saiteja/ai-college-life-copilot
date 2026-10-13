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

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RagRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RagRetrievalService.class);

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final GeminiEmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private final double defaultThreshold;

    @Autowired
    public RagRetrievalService(DocumentRepository documentRepository,
                               DocumentChunkRepository documentChunkRepository,
                               GeminiEmbeddingService embeddingService,
                               @Value("${rag.similarity-threshold:0.10}") double defaultThreshold) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingService = embeddingService;
        this.defaultThreshold = defaultThreshold;
        this.objectMapper = JsonMapper.builder().build();
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "what", "when", "where", "which", "who", "whom", "whose", "why", "how",
            "does", "do", "did", "have", "has", "had", "this", "that", "these", "those",
            "about", "with", "from", "into", "during", "before", "after", "above", "below",
            "more", "most", "other", "some", "such", "only", "same", "than", "too", "very",
            "can", "will", "just", "should", "now", "the", "a", "an", "is", "are", "was", "were"
    );

    private double computeLexicalOverlap(String query, String content) {
        if (query == null || content == null) return 0.0;
        String[] qTokens = query.toLowerCase().split("\\W+");
        List<String> terms = Arrays.stream(qTokens)
                .filter(t -> t.length() > 2 && !STOP_WORDS.contains(t))
                .toList();

        if (terms.isEmpty()) return 0.0;

        String contentLower = content.toLowerCase();
        int matched = 0;
        for (String t : terms) {
            if (contentLower.contains(t)) {
                matched++;
            }
        }

        if (matched == 0) return 0.0;
        double ratio = (double) matched / terms.size();
        return ratio >= 1.0 ? 0.85 : ratio * 0.70;
    }

    public List<ChunkDto> retrieveRelevantChunks(String query) {
        return retrieveRelevantChunks(query, 3, defaultThreshold);
    }

    public List<ChunkDto> retrieveRelevantChunks(String query, int topK, double threshold) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        List<DocumentChunk> chunks = documentChunkRepository.findAll();
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, String> docTitleMap = documentRepository.findAll().stream()
                .collect(Collectors.toMap(Document::getId, Document::getTitle, (a, b) -> a));

        GeminiEmbeddingService.EmbeddingResult queryEmbedding;
        try {
            queryEmbedding = embeddingService.generateSemanticEmbedding(query);
        } catch (Exception e) {
            log.error("Failed to generate semantic embedding for query: {}", e.getMessage());
            return Collections.emptyList();
        }

        String expectedModel = embeddingService.getEmbeddingModel();
        int expectedDim = embeddingService.getEmbeddingDimension();
        String expectedVer = embeddingService.getEmbeddingVersion();

        List<ChunkDto> scored = new ArrayList<>();
        int skippedCount = 0;

        for (DocumentChunk chunk : chunks) {
            double[] chunkVec = parseVectorEmbedding(chunk.getVectorEmbedding(), expectedDim);

            boolean isModelValid = expectedModel.equalsIgnoreCase(chunk.getEmbeddingModel());
            boolean isDimValid = (chunk.getEmbeddingDimension() != null && chunk.getEmbeddingDimension() == expectedDim)
                    || (chunkVec != null && chunkVec.length == expectedDim);
            boolean isVerValid = expectedVer.equalsIgnoreCase(chunk.getEmbeddingVersion());

            if (!isModelValid || !isDimValid || !isVerValid || chunkVec == null || chunkVec.length != expectedDim) {
                skippedCount++;
                continue;
            }

            double vectorScore = embeddingService.cosineSimilarity(queryEmbedding.values(), chunkVec);
            double lexicalScore = computeLexicalOverlap(query, chunk.getContent());
            double score = Math.max(vectorScore, lexicalScore);
            String docTitle = docTitleMap.getOrDefault(chunk.getDocumentId(), "College Policy Document");

            scored.add(new ChunkDto(
                    chunk.getId(),
                    chunk.getDocumentId(),
                    docTitle,
                    chunk.getPageNumber() != null ? chunk.getPageNumber() : 1,
                    chunk.getChunkIndex() != null ? chunk.getChunkIndex() : 0,
                    chunk.getContent(),
                    chunk.getVectorEmbedding(),
                    chunk.getEmbeddingModel(),
                    chunk.getEmbeddingDimension() != null ? chunk.getEmbeddingDimension() : expectedDim,
                    chunk.getEmbeddingVersion(),
                    score
            ));
        }

        if (skippedCount > 0 && scored.isEmpty()) {
            log.warn("All candidate chunks skipped due to unmigrated embedding vectors. Re-indexing required.");
        }

        // Sort descending by cosine similarity score
        scored.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // Filter meeting threshold
        List<ChunkDto> relevant = scored.stream()
                .filter(c -> c.getScore() >= threshold)
                .collect(Collectors.toList());

        return relevant.subList(0, Math.min(topK, relevant.size()));
    }

    private double[] parseVectorEmbedding(String rawJson, int expectedDim) {
        if (rawJson == null || rawJson.isBlank()) return null;

        try {
            JsonNode node = objectMapper.readTree(rawJson);
            if (node.has("values")) {
                node = node.get("values");
            }
            if (node.isArray() && node.size() == expectedDim) {
                double[] vec = new double[node.size()];
                for (int i = 0; i < node.size(); i++) {
                    vec[i] = node.get(i).asDouble();
                }
                return vec;
            }
        } catch (Exception e) {
            // Malformed JSON embedding
        }

        return null;
    }
}
