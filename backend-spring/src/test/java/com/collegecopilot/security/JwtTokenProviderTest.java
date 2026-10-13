package com.collegecopilot.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider("test_secret_key_for_testing_purposes_only_12345");
    }

    @Test
    @DisplayName("Generate token with claims and successfully verify it")
    void testGenerateAndValidateToken() {
        String token = tokenProvider.generateToken(
                "user-123",
                "Jane Doe",
                "jane@example.com",
                "STUDENT",
                "std-456",
                null
        );

        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();

        DecodedJWT decoded = tokenProvider.verifyAndDecode(token);
        assertThat(decoded).isNotNull();
        assertThat(decoded.getSubject()).isEqualTo("user-123");
        assertThat(decoded.getClaim("name").asString()).isEqualTo("Jane Doe");
        assertThat(decoded.getClaim("email").asString()).isEqualTo("jane@example.com");
        assertThat(decoded.getClaim("role").asString()).isEqualTo("STUDENT");
        assertThat(decoded.getClaim("studentId").asString()).isEqualTo("std-456");
        assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo("user-123");
    }

    @Test
    @DisplayName("Invalid or tampered token returns false")
    void testInvalidToken() {
        assertThat(tokenProvider.validateToken("invalid.token.here")).isFalse();
        assertThat(tokenProvider.verifyAndDecode("invalid.token.here")).isNull();
        assertThat(tokenProvider.getUserIdFromToken("invalid.token.here")).isNull();
    }

    @Test
    @DisplayName("Production startup fails safely when JWT_SECRET is unset or blank")
    void testProductionStartupFailsWhenSecretUnset() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                new JwtTokenProvider("", "production")
        );
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                new JwtTokenProvider(null, "production")
        );
    }

    @Test
    @DisplayName("Production startup fails safely when JWT_SECRET uses default development secret")
    void testProductionStartupFailsWhenDefaultDevSecretUsed() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                new JwtTokenProvider("ai_college_copilot_secret_key_jwt_2026", "production")
        );
    }

    @Test
    @DisplayName("Production startup fails safely when JWT_SECRET is weak (< 32 characters)")
    void testProductionStartupFailsWhenSecretTooShort() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                new JwtTokenProvider("short_secret_under_32_chars!", "production")
        );
    }

    @Test
    @DisplayName("Production startup succeeds with strong >= 32 char secret")
    void testProductionStartupSucceedsWithStrongSecret() {
        JwtTokenProvider prodProvider = new JwtTokenProvider(
                "super_strong_production_jwt_secret_key_2026_safe_and_secure",
                "production"
        );
        String token = prodProvider.generateToken("u1", "Admin", "adm@test.edu", "ADMIN", null, "a1");
        assertThat(prodProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Development mode safely uses fallback default secret when unset")
    void testDevelopmentFallbackWhenUnset() {
        JwtTokenProvider devProvider = new JwtTokenProvider("", "development");
        String token = devProvider.generateToken("u2", "Dev", "dev@test.edu", "STUDENT", "s2", null);
        assertThat(devProvider.validateToken(token)).isTrue();
    }
}
