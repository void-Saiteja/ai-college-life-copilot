package com.collegecopilot.health;

import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.security.JwtAuthenticationFilter;
import com.collegecopilot.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HealthSecurityIntegrationTest {

    private MockMvc mockMvc;
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        HealthController healthController = new HealthController("production", "1.0.0");
        mockMvc = MockMvcBuilders.standaloneSetup(healthController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/health passes through JwtAuthenticationFilter without token and returns 200")
    void testPublicHealthEndpointWithoutTokenReturns200() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("AI College Life Copilot Backend API is running smoothly."))
                .andExpect(jsonPath("$.environment").value("production"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.dbPassword").doesNotExist())
                .andExpect(jsonPath("$.apiKey").doesNotExist());
    }

    @Test
    @DisplayName("GET /health alias passes through JwtAuthenticationFilter without token and returns 200")
    void testHealthAliasWithoutTokenReturns200() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    @DisplayName("GET /api/health also succeeds when valid Authorization token is provided")
    void testHealthEndpointWithTokenReturns200() throws Exception {
        String token = tokenProvider.generateToken("u-1", "Test Student", "test@student.edu", "STUDENT", "std-1", null);

        mockMvc.perform(get("/api/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("OK"));
    }
}
