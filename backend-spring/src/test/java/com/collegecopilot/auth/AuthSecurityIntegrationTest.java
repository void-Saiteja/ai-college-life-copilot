package com.collegecopilot.auth;

import com.collegecopilot.auth.dto.AuthResponse;
import com.collegecopilot.auth.dto.UserSummaryDto;
import com.collegecopilot.config.GlobalExceptionHandler;
import com.collegecopilot.security.JwtAuthenticationFilter;
import com.collegecopilot.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthSecurityIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/auth/me with valid Bearer token returns 200 with student profile")
    void testGetMeWithValidToken() throws Exception {
        String token = tokenProvider.generateToken("u-test-1", "Test Student", "test@student.edu", "STUDENT", "std-test-1", null);

        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id("u-test-1")
                .name("Test Student")
                .email("test@student.edu")
                .role("STUDENT")
                .studentId("std-test-1")
                .studentCode("CS-9999")
                .semester(2)
                .department("Computer Science")
                .gpa(new BigDecimal("3.95"))
                .build();

        when(authService.getMe("u-test-1")).thenReturn(new AuthResponse(true, userSummary));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.id").value("u-test-1"))
                .andExpect(jsonPath("$.user.name").value("Test Student"))
                .andExpect(jsonPath("$.user.email").value("test@student.edu"))
                .andExpect(jsonPath("$.user.studentCode").value("CS-9999"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/auth/me with invalid Bearer token returns 401")
    void testGetMeWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer bad.token.here")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized: Missing or invalid token"));
    }

    @Test
    @DisplayName("GET /api/auth/me without Authorization header returns 401")
    void testGetMeWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized: Missing or invalid token"));
    }
}
