package com.collegecopilot.auth;

import com.collegecopilot.auth.dto.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
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
    @DisplayName("POST /api/auth/register succeeds with valid payload")
    void testRegisterSuccess() throws Exception {
        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id("u-123")
                .name("Alex Mercer")
                .email("alex@student.edu")
                .role("STUDENT")
                .studentId("std-123")
                .build();
        AuthResponse response = new AuthResponse(true, "Registration successful", "token-xyz", userSummary);

        when(authService.register(any(RegisterRequest.class), any())).thenReturn(response);

        String json = """
                {
                  "name": "Alex Mercer",
                  "email": "alex@student.edu",
                  "password": "password123",
                  "role": "STUDENT"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.token").value("token-xyz"))
                .andExpect(jsonPath("$.user.email").value("alex@student.edu"));
    }

    @Test
    @DisplayName("POST /api/auth/register returns 409 on duplicate email")
    void testRegisterDuplicate() throws Exception {
        when(authService.register(any(RegisterRequest.class), any()))
                .thenThrow(new AuthException(HttpStatus.CONFLICT, "User with this email already exists"));

        String json = """
                {
                  "name": "Alex Mercer",
                  "email": "existing@student.edu",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("User with this email already exists"));
    }

    @Test
    @DisplayName("POST /api/auth/register returns 400 when required fields missing or password too short")
    void testRegisterInvalidPayload() throws Exception {
        String json = """
                {
                  "name": "",
                  "email": "not-an-email",
                  "password": "123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login succeeds with valid credentials")
    void testLoginSuccess() throws Exception {
        UserSummaryDto userSummary = UserSummaryDto.builder()
                .id("u-123")
                .name("Alex Mercer")
                .email("alex@student.edu")
                .role("STUDENT")
                .studentId("std-123")
                .semester(4)
                .department("Computer Science")
                .build();
        AuthResponse response = new AuthResponse(true, "Login successful", "jwt-token-abc", userSummary);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        String json = """
                {
                  "email": "alex@student.edu",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.token").value("jwt-token-abc"))
                .andExpect(jsonPath("$.user.id").value("u-123"));
    }

    @Test
    @DisplayName("POST /api/auth/login returns 401 on invalid credentials")
    void testLoginInvalidCredentials() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        String json = """
                {
                  "email": "alex@student.edu",
                  "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    @DisplayName("GET /api/auth/me without authentication returns 401")
    void testGetMeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Unauthorized: Missing or invalid token"));
    }
}
