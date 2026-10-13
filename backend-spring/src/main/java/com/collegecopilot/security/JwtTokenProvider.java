package com.collegecopilot.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Generates and verifies JWT tokens matching Node.js backend behavior.
 * Uses HMAC256 with secret resolved from environment variable JWT_SECRET.
 * Never logs or exposes the secret key.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String DEFAULT_DEV_SECRET = "ai_college_copilot_secret_key_jwt_2026";
    private static final long EXPIRATION_TIME_MS = 7L * 24 * 60 * 60 * 1000; // 7 days

    private final String jwtSecret;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    @Autowired
    public JwtTokenProvider(@Value("${JWT_SECRET:}") String jwtSecret,
                            @Value("${NODE_ENV:${ENVIRONMENT:${spring.profiles.active:development}}}") String environment) {
        boolean isProduction = isProductionEnvironment(environment);
        String resolvedSecret;

        if (isProduction) {
            if (jwtSecret == null || jwtSecret.isBlank()) {
                throw new IllegalStateException("FATAL SECURITY ERROR: Production environment detected, but JWT_SECRET is unset. A strong, unique JWT_SECRET must be configured via environment variables in production.");
            }
            String trimmed = jwtSecret.trim();
            if (DEFAULT_DEV_SECRET.equals(trimmed)) {
                throw new IllegalStateException("FATAL SECURITY ERROR: Production environment detected, but JWT_SECRET is using the insecure default development fallback secret. A strong, unique JWT_SECRET must be configured in production.");
            }
            if (trimmed.length() < 32) {
                throw new IllegalStateException("FATAL SECURITY ERROR: Production environment detected, but JWT_SECRET is too weak (length must be at least 32 characters). A strong secret is required in production.");
            }
            resolvedSecret = trimmed;
        } else {
            if (jwtSecret != null && !jwtSecret.isBlank()) {
                resolvedSecret = jwtSecret.trim();
            } else {
                log.warn("JWT_SECRET unset in non-production environment; using development fallback secret. NEVER use this in production.");
                resolvedSecret = DEFAULT_DEV_SECRET;
            }
        }

        this.jwtSecret = resolvedSecret;
        this.algorithm = Algorithm.HMAC256(resolvedSecret);
        this.verifier = JWT.require(this.algorithm).build();
        log.info("JwtTokenProvider initialized with HMAC256 algorithm");
    }

    public JwtTokenProvider(String jwtSecret) {
        this(jwtSecret, "development");
    }

    private static boolean isProductionEnvironment(String env) {
        if (env == null || env.isBlank()) return false;
        String lower = env.trim().toLowerCase();
        return lower.contains("prod") || lower.contains("production");
    }

    public String generateToken(String userId, String name, String email, String role, String studentId, String adminId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME_MS);

        return JWT.create()
                .withSubject(userId)
                .withClaim("id", userId)
                .withClaim("name", name)
                .withClaim("email", email)
                .withClaim("role", role)
                .withClaim("studentId", studentId)
                .withClaim("adminId", adminId)
                .withIssuedAt(now)
                .withExpiresAt(expiryDate)
                .sign(algorithm);
    }

    public DecodedJWT verifyAndDecode(String token) {
        try {
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            log.debug("JWT verification failed: {}", e.getMessage());
            return null;
        }
    }

    public boolean validateToken(String token) {
        return verifyAndDecode(token) != null;
    }

    public String getUserIdFromToken(String token) {
        DecodedJWT decoded = verifyAndDecode(token);
        if (decoded == null) return null;
        String idClaim = decoded.getClaim("id").asString();
        return idClaim != null ? idClaim : decoded.getSubject();
    }
}
