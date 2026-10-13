package com.collegecopilot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads development environment variables from .env if present.
 * Ensures properties like DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, DB_SSL_CA_CERT
 * are available to Spring Boot without hardcoding secrets in source control.
 *
 * System environment variables always take precedence.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String[] candidatePaths = {
            ".env",
            "backend-spring/.env",
            "../backend/.env",
            "backend/.env",
            "../.env"
        };

        for (String path : candidatePaths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try {
                    Map<String, Object> props = parseDotenv(file);
                    if (!props.isEmpty()) {
                        log.info("Loaded {} environment properties from {}", props.size(), file.getCanonicalPath());
                        // addLast ensures OS environment variables take precedence
                        environment.getPropertySources().addLast(new MapPropertySource("dotenvProperties:" + path, props));
                        break;
                    }
                } catch (IOException e) {
                    log.warn("Failed to read .env file at {}: {}", path, e.getMessage());
                }
            }
        }
    }

    private Map<String, Object> parseDotenv(File file) throws IOException {
        Map<String, Object> props = new HashMap<>();
        List<String> lines = Files.readAllLines(file.toPath());
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eqIdx = trimmed.indexOf('=');
            if (eqIdx > 0) {
                String key = trimmed.substring(0, eqIdx).trim();
                String value = trimmed.substring(eqIdx + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                // If resolving DB_SSL_CA_PATH, resolve relative to the .env file directory if needed
                if ("DB_SSL_CA_PATH".equals(key)) {
                    File certFile = new File(value);
                    if (!certFile.exists() && file.getParentFile() != null) {
                        File relativeCert = new File(file.getParentFile(), value);
                        if (relativeCert.exists()) {
                            value = relativeCert.getAbsolutePath();
                        }
                    }
                }
                props.put(key, value);
            }
        }
        return props;
    }
}
