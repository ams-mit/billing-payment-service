package com.ams.billing.test;

import com.ams.billing.dto.response.ApiResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * TEST ONLY — generates JWT tokens for Postman testing.
 * Only active when spring.profiles.active=dev or test.
 * Remove or disable before production deployment.
 */
@Hidden                          // hidden from Swagger UI
@Slf4j
@RestController
@RequestMapping("/test/token")
@Profile({"dev", "test"})        // never runs in prod profile
public class TestTokenController {

    @Value("${app.jwt.secret:default-secret-key-that-is-at-least-32-characters-long}")
    private String jwtSecret;

    @GetMapping
    public ResponseEntity<String> generateToken(
            @RequestParam String userId,
            @RequestParam String role,
            HttpServletRequest request) {

        try {
            log.info("Generating token for userId: {}, role: {}", userId, role);
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            String token = Jwts.builder()
                    .subject(userId)
                    .claim("roles", List.of(role))
                    .issuedAt(new Date())
                    .expiration(new Date(System.currentTimeMillis() + 86400000))
                    .signWith(key)
                    .compact();

            return ResponseEntity.ok("Token: " + token);
        } catch (Exception e) {
            log.error("Token generation failed: ", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}