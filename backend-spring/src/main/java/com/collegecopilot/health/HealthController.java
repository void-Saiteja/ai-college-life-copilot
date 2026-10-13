package com.collegecopilot.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class HealthController {

    private final String environment;
    private final String version;

    public HealthController(
            @Value("${app.environment:${spring.profiles.active:${NODE_ENV:production}}}") String environment,
            @Value("${app.version:1.0.0}") String version) {
        this.environment = (environment != null && !environment.isBlank()) ? environment : "production";
        this.version = (version != null && !version.isBlank()) ? version : "1.0.0";
    }

    @GetMapping({"/api/health", "/health"})
    public ResponseEntity<HealthResponse> getHealth() {
        HealthResponse response = new HealthResponse(
                true,
                "OK",
                "AI College Life Copilot Backend API is running smoothly.",
                Instant.now().toString(),
                environment,
                version
        );
        return ResponseEntity.ok(response);
    }
}
