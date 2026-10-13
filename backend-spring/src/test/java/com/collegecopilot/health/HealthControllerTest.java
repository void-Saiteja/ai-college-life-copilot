package com.collegecopilot.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HealthControllerTest {

    private MockMvc mockMvc;
    private HealthController healthController;

    @BeforeEach
    void setUp() {
        healthController = new HealthController("production", "1.0.0");
        mockMvc = MockMvcBuilders.standaloneSetup(healthController).build();
    }

    @Test
    @DisplayName("GET /api/health returns HTTP 200 with OK status and metadata")
    void testGetApiHealthReturns200() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("AI College Life Copilot Backend API is running smoothly."))
                .andExpect(jsonPath("$.environment").value("production"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                // Ensure no sensitive fields or misleading claims are leaked
                .andExpect(jsonPath("$.dbPassword").doesNotExist())
                .andExpect(jsonPath("$.apiKey").doesNotExist())
                .andExpect(jsonPath("$.gemini").doesNotExist())
                .andExpect(jsonPath("$.database").doesNotExist());
    }

    @Test
    @DisplayName("GET /health alias returns HTTP 200 with OK status")
    void testGetHealthAliasReturns200() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("HealthController uses default values when empty/null parameters are passed")
    void testHealthControllerDefaults() throws Exception {
        HealthController defaultController = new HealthController(null, null);
        MockMvc defaultMockMvc = MockMvcBuilders.standaloneSetup(defaultController).build();

        defaultMockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environment").value("production"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }
}
