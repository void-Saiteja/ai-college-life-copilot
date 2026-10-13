package com.collegecopilot.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.UUID;

/**
 * Datasource configuration for connecting to Aiven MySQL with SSL/TLS encryption.
 * Credentials are supplied via environment variables (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, DB_SSL_CA_CERT).
 * Secrets and passwords are never hardcoded or logged.
 */
@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${DB_HOST:localhost}")
    private String dbHost;

    @Value("${DB_PORT:3306}")
    private int dbPort;

    @Value("${DB_NAME:defaultdb}")
    private String dbName;

    @Value("${DB_USER:root}")
    private String dbUser;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    @Value("${DB_SSL:true}")
    private String dbSsl;

    @Value("${DB_SSL_CA_CERT:}")
    private String dbSslCaCert;

    @Value("${DB_SSL_CA_PATH:}")
    private String dbSslCaPath;

    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("Initializing DataSource for host={}:{}, database={}, user={}", dbHost, dbPort, dbName, dbUser);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("AivenMySQLHikariPool");
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setUsername(dbUser);
        hikariConfig.setPassword(dbPassword);

        // Connection pool tuning
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(20000);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setValidationTimeout(5000);

        // Build JDBC URL with appropriate SSL/TLS configuration
        String jdbcUrl = buildJdbcUrl();
        hikariConfig.setJdbcUrl(jdbcUrl);

        return new HikariDataSource(hikariConfig);
    }

    private String buildJdbcUrl() {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("jdbc:mysql://")
                .append(dbHost)
                .append(":")
                .append(dbPort)
                .append("/")
                .append(dbName)
                .append("?useUnicode=true")
                .append("&characterEncoding=UTF-8")
                .append("&serverTimezone=UTC")
                .append("&allowPublicKeyRetrieval=true");

        boolean sslEnabled = !"false".equalsIgnoreCase(dbSsl);

        if (!sslEnabled) {
            log.info("Database SSL disabled by configuration (sslMode=DISABLED)");
            urlBuilder.append("&sslMode=DISABLED");
            return urlBuilder.toString();
        }

        // Check if custom CA certificate is provided
        File trustStoreFile = tryCreateTrustStoreFromCa();
        if (trustStoreFile != null && trustStoreFile.exists()) {
            log.info("Aiven CA certificate configured. Connecting with sslMode=VERIFY_CA");
            try {
                urlBuilder.append("&sslMode=VERIFY_CA")
                        .append("&trustCertificateKeyStoreUrl=").append(trustStoreFile.toURI().toURL())
                        .append("&trustCertificateKeyStorePassword=aivenTrustStorePassword")
                        .append("&trustCertificateKeyStoreType=PKCS12");
            } catch (Exception e) {
                log.warn("Failed to format truststore URL, falling back to sslMode=REQUIRED: {}", e.getMessage());
                urlBuilder.append("&sslMode=REQUIRED");
            }
        } else {
            log.info("Connecting to Aiven MySQL with encrypted TLS connection (sslMode=REQUIRED)");
            urlBuilder.append("&sslMode=REQUIRED");
        }

        return urlBuilder.toString();
    }

    private File tryCreateTrustStoreFromCa() {
        byte[] certBytes = null;

        // 1. Check direct certificate string in DB_SSL_CA_CERT
        if (dbSslCaCert != null && !dbSslCaCert.isBlank()) {
            String trimmed = dbSslCaCert.trim();
            if (trimmed.contains("-----BEGIN CERTIFICATE-----")) {
                certBytes = trimmed.getBytes(StandardCharsets.UTF_8);
            } else {
                // If it might be a file path
                File file = new File(trimmed);
                if (file.exists() && file.isFile()) {
                    try {
                        certBytes = Files.readAllBytes(file.toPath());
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        // 2. Check file path in DB_SSL_CA_PATH
        if (certBytes == null && dbSslCaPath != null && !dbSslCaPath.isBlank()) {
            File file = new File(dbSslCaPath.trim());
            if (file.exists() && file.isFile()) {
                try {
                    certBytes = Files.readAllBytes(file.toPath());
                } catch (IOException ignored) {
                }
            }
        }

        // 3. Fallback: check ca.pem in common project locations if not specified
        if (certBytes == null) {
            String[] commonCaLocations = {"ca.pem", "backend/ca.pem", "../backend/ca.pem", "../ca.pem"};
            for (String loc : commonCaLocations) {
                File file = new File(loc);
                if (file.exists() && file.isFile()) {
                    try {
                        certBytes = Files.readAllBytes(file.toPath());
                        log.info("Located Aiven CA certificate at {}", file.getCanonicalPath());
                        break;
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        if (certBytes == null || certBytes.length == 0) {
            return null;
        }

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));

            KeyStore ks = KeyStore.getInstance("PKCS12");
            char[] password = "aivenTrustStorePassword".toCharArray();
            ks.load(null, password);
            ks.setCertificateEntry("aiven-ca", cert);

            File tempKeystore = File.createTempFile("aiven-ca-" + UUID.randomUUID(), ".p12");
            tempKeystore.deleteOnExit();

            try (FileOutputStream fos = new FileOutputStream(tempKeystore)) {
                ks.store(fos, password);
            }

            return tempKeystore;
        } catch (Exception e) {
            log.warn("Could not create PKCS12 truststore from CA certificate: {}. Proceeding with sslMode=REQUIRED.", e.getMessage());
            return null;
        }
    }
}
