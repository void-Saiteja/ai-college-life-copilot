package com.collegecopilot.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiEmbeddingServiceTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    @Test
    @DisplayName("generateSemanticEmbedding produces 768-dimension vectors")
    void generateSemanticEmbedding_returns768Dimensions() {
        GeminiEmbeddingService service = new GeminiEmbeddingService("mock_key", "gemini-embedding-2", 768, "v1", mockHttpClient);

        GeminiEmbeddingService.EmbeddingResult result = service.generateSemanticEmbedding("Attendance policy in computer science");

        assertNotNull(result);
        assertNotNull(result.values());
        assertEquals(768, result.values().length);
        assertEquals("gemini-embedding-2", result.model());
        assertEquals("v1", result.version());
    }

    @Test
    @DisplayName("cosineSimilarity computes correct vector alignment")
    void cosineSimilarity_computesExpectedScores() {
        GeminiEmbeddingService service = new GeminiEmbeddingService("mock_key", "gemini-embedding-2", 768, "v1", mockHttpClient);

        double[] vec1 = new double[]{1.0, 0.0, 0.0};
        double[] vec2 = new double[]{1.0, 0.0, 0.0};
        assertEquals(1.0, service.cosineSimilarity(vec1, vec2), 0.0001);

        double[] orthogonal = new double[]{0.0, 1.0, 0.0};
        assertEquals(0.0, service.cosineSimilarity(vec1, orthogonal), 0.0001);

        double[] opposite = new double[]{-1.0, 0.0, 0.0};
        assertEquals(-1.0, service.cosineSimilarity(vec1, opposite), 0.0001);

        // Mismatched vector dimensions return 0.0
        double[] mismatched = new double[]{1.0, 0.0};
        assertEquals(0.0, service.cosineSimilarity(vec1, mismatched));
    }

    @Test
    @DisplayName("Null or empty text returns zero vector")
    void generateSemanticEmbedding_emptyText_returnsZeroVector() {
        GeminiEmbeddingService service = new GeminiEmbeddingService("mock_key", "gemini-embedding-2", 768, "v1", mockHttpClient);

        GeminiEmbeddingService.EmbeddingResult result = service.generateSemanticEmbedding("");
        assertNotNull(result);
        assertEquals(768, result.values().length);
        for (double v : result.values()) {
            assertEquals(0.0, v);
        }
    }

    @Test
    @DisplayName("Real Gemini API call parses 768-dim response values")
    void generateSemanticEmbedding_geminiSuccess_parses768Values() throws Exception {
        StringBuilder jsonValues = new StringBuilder("[");
        for (int i = 0; i < 768; i++) {
            jsonValues.append(0.01 * (i % 10));
            if (i < 767) jsonValues.append(",");
        }
        jsonValues.append("]");

        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("{\"embedding\":{\"values\":" + jsonValues + "}}");
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(HttpRequest.class), any());

        GeminiEmbeddingService service = new GeminiEmbeddingService("real_key_123", "gemini-embedding-2", 768, "v1", mockHttpClient);

        GeminiEmbeddingService.EmbeddingResult result = service.generateSemanticEmbedding("Campus safety rules");

        assertEquals("google", result.provider());
        assertEquals(768, result.values().length);
    }
}
