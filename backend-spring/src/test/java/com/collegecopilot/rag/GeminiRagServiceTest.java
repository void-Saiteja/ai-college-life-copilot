package com.collegecopilot.rag;

import com.collegecopilot.rag.dto.ChunkDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiRagServiceTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    @Test
    @DisplayName("When document chunks contain the answer, Gemini returns verified FOUND status with grounded answer")
    void answerAndVerify_foundStatus_returnsGroundedAnswer() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"status\\":\\"FOUND\\",\\"answer\\":\\"The minimum attendance requirement is 75% according to College Handbook (Page 3).\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """);
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(HttpRequest.class), any());

        GeminiRagService service = new GeminiRagService("valid_gemini_key", mockHttpClient);

        ChunkDto chunk = new ChunkDto("chk-1", "doc-1", "College Handbook", 3, 0,
                "Attendance requirement is 75% for all courses.", null, "gemini-embedding-2", 768, "v1", 0.88);

        GeminiRagService.GroundingVerificationResult result = service.answerAndVerifyDocumentQuery(
                "What is the minimum attendance requirement?",
                List.of(chunk)
        );

        assertTrue(result.success());
        assertEquals("FOUND", result.status());
        assertTrue(result.answer().contains("75%"));
        assertTrue(result.answer().contains("Page 3"));
    }

    @Test
    @DisplayName("When question asks about topic not covered in excerpts, Gemini returns NOT_FOUND (hallucination defense)")
    void answerAndVerify_notCoveredTopic_returnsNotFound() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"status\\":\\"NOT_FOUND\\",\\"answer\\":\\"\\"}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """);
        doReturn(mockHttpResponse).when(mockHttpClient).send(any(HttpRequest.class), any());

        GeminiRagService service = new GeminiRagService("valid_gemini_key", mockHttpClient);

        ChunkDto chunk = new ChunkDto("chk-1", "doc-1", "Parking Regulations", 1, 0,
                "Students may park standard automobiles in Lot B.", null, "gemini-embedding-2", 768, "v1", 0.45);

        GeminiRagService.GroundingVerificationResult result = service.answerAndVerifyDocumentQuery(
                "Can I land a private helicopter on the college campus rooftop?",
                List.of(chunk)
        );

        assertTrue(result.success());
        assertEquals("NOT_FOUND", result.status());
        assertEquals("", result.answer());
    }

    @Test
    @DisplayName("Empty chunks list immediately returns NOT_FOUND without invoking network")
    void answerAndVerify_emptyChunks_returnsNotFoundImmediately() throws Exception {
        GeminiRagService service = new GeminiRagService("valid_gemini_key", mockHttpClient);

        GeminiRagService.GroundingVerificationResult result = service.answerAndVerifyDocumentQuery(
                "Any policy?",
                Collections.emptyList()
        );

        assertTrue(result.success());
        assertEquals("NOT_FOUND", result.status());
        verify(mockHttpClient, never()).send(any(), any());
    }

    @Test
    @DisplayName("formatDocumentContext includes document title and exact page numbers")
    void formatDocumentContext_formatsExcerptsWithPageNumbers() {
        GeminiRagService service = new GeminiRagService("valid_gemini_key", mockHttpClient);

        ChunkDto c1 = new ChunkDto("c-1", "doc-1", "Exam Code", 4, 0, "No cell phones in exams", null, "m", 768, "v1", 0.9);
        ChunkDto c2 = new ChunkDto("c-2", "doc-1", "Exam Code", 5, 1, "Appeals window is 48h", null, "m", 768, "v1", 0.85);

        String formatted = service.formatDocumentContext(List.of(c1, c2));

        assertTrue(formatted.contains("Source Document: \"Exam Code\" (Page 4)"));
        assertTrue(formatted.contains("Source Document: \"Exam Code\" (Page 5)"));
        assertTrue(formatted.contains("No cell phones in exams"));
    }

    @Test
    @DisplayName("When all live Gemini models fail/time out, falls back to deterministic excerpt verification")
    void answerAndVerify_allModelsFail_usesDeterministicFallback() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new java.io.IOException("Connection timed out"));

        GeminiRagService service = new GeminiRagService("valid_gemini_key", mockHttpClient);

        ChunkDto chunk = new ChunkDto("chk-1", "doc-1", "Cancellation Policy 2026", 1, 0,
                "Application form for cancellation and withdrawal must be submitted within 15 days.", null, "gemini-embedding-2", 768, "v1", 0.85);

        GeminiRagService.GroundingVerificationResult result = service.answerAndVerifyDocumentQuery(
                "What is the cancellation policy?",
                List.of(chunk)
        );

        assertTrue(result.success());
        assertEquals("FOUND", result.status());
        assertFalse(result.isServiceError());
        assertTrue(result.answer().contains("[Source-Verified Excerpt (AI Offline)]"));
        assertTrue(result.answer().contains("Cancellation Policy 2026"));
        assertTrue(result.answer().contains("15 days"));
    }

    @Test
    @DisplayName("When all live Gemini models fail and excerpts do not cover topic, returns NOT_FOUND")
    void answerAndVerify_allModelsFailUnrelatedTopic_returnsNotFound() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new java.io.IOException("HTTP 429 Quota Exhausted"));

        GeminiRagService service = new GeminiRagService("valid_gemini_key", mockHttpClient);

        ChunkDto chunk = new ChunkDto("chk-1", "doc-1", "Hostel Rules", 2, 0,
                "Quiet hours begin at 10 PM in residential halls.", null, "gemini-embedding-2", 768, "v1", 0.40);

        GeminiRagService.GroundingVerificationResult result = service.answerAndVerifyDocumentQuery(
                "Can I pilot an airplane over the campus?",
                List.of(chunk)
        );

        assertTrue(result.success());
        assertEquals("NOT_FOUND", result.status());
        assertEquals("", result.answer());
    }
}
