package com.collegecopilot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BackendSpringApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Spring ApplicationContext loads with DataSource configured")
    void contextLoads() {
        assertThat(dataSource).isNotNull();
    }

    @Test
    @DisplayName("Verify Spring Boot establishes valid SSL/TLS connection to Aiven MySQL")
    void testAivenMySQLConnection() throws Exception {
        assertThat(dataSource).isNotNull();

        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn).isNotNull();
            assertThat(conn.isValid(10)).isTrue();

            DatabaseMetaData metaData = conn.getMetaData();
            assertThat(metaData.getDatabaseProductName()).containsIgnoringCase("MySQL");

            // Perform non-mutating ping query
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1 AS ping_val, DATABASE() AS active_db, VERSION() AS db_ver")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt("ping_val")).isEqualTo(1);
                assertThat(rs.getString("active_db")).isNotEmpty();
            }
        }
    }
}
