package com.ams.billing.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerStartupLogger implements CommandLineRunner {

    private final Environment env;
    private final JdbcTemplate jdbcTemplate;

    @Value("${server.port:8081}")
    private String port;

    @Override
    public void run(String... args) {
        log.info("========================================================================");
        log.info("🚀 BILLING PAYMENT SERVICE STARTUP DETAILS");
        log.info("========================================================================");
        log.info("Server Port    : {}", port);
        log.info("Base URL      : http://localhost:{}", port);
        log.info("Active Profiles: {}", Arrays.toString(env.getActiveProfiles()));

        try {
            String dbUrl = env.getProperty("spring.datasource.url");
            log.info("Database URL  : {}", dbUrl);

            // Test database connection
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                log.info("Database Conn  : ✅ CONNECTED");
            } else {
                log.info("Database Conn  : ❌ FAILED (unexpected result)");
            }
        } catch (Exception e) {
            log.error("Database Conn  : ❌ FAILED - {}", e.getMessage());
        }

        log.info("JWT Secret Set : {}", env.getProperty("app.jwt.secret") != null ? "YES" : "NO (using default)");
        log.info("========================================================================");
    }
}
