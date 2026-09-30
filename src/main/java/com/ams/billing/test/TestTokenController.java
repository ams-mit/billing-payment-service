package com.ams.billing.test;

import com.ams.billing.dto.response.ApiResponse;
import io.jsonwebtoken.Jwts;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * TEST ONLY — generates JWT tokens for Postman testing.
 *
 * Active ONLY on dev and test profiles.
 * Hidden from Swagger UI.
 * Never deployed to production.
 *
 * Endpoints:
 *   POST /test/token          → generates a user JWT (type: user)
 *   POST /test/service-token  → generates a service JWT (type: service)
 */
@Slf4j
@Hidden
@RestController
@RequestMapping("/test")
@Profile({"dev", "test"})
public class TestTokenController {

    // POST /test/token
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateUserToken(
            @RequestParam String userId,
            @RequestParam String role,
            HttpServletRequest httpRequest) {

        try {
            RSAPrivateKey gatewayPrivateKey = DevKeyLoader.loadPrivateKey(
                    "keys/gateway-private.pem");

            String token = Jwts.builder()
                    .subject(userId)
                    .claim("type", "user")
                    .claim("roles", List.of(role))
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(86400)))
                    .signWith(gatewayPrivateKey, Jwts.SIG.RS256)
                    .compact();

            Map<String, String> data = Map.of(
                    "token", token,
                    "userId", userId,
                    "role", role,
                    "type", "user",
                    "usage", "Authorization: Bearer " + token
            );

            log.info("Test user token generated: userId={}, role={}", userId, role);

            return ResponseEntity.ok(ApiResponse.ok(
                    "Test token generated — dev/test profile only",
                    data,
                    httpRequest.getHeader("X-Request-ID")));

        } catch (Exception ex) {
            log.error("Failed to generate test token: {}", ex.getMessage(), ex);
            throw new RuntimeException(
                    "Failed to generate test token. " +
                            "Check that src/test/resources/keys/gateway-private.pem exists. " +
                            "Run the key generation commands in the setup guide.", ex);
        }
    }

    // POST /test/service-token
    @PostMapping("/service-token")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateServiceToken(
            @RequestParam String serviceName,
            HttpServletRequest httpRequest) {

        try {
            RSAPrivateKey gatewayPrivateKey = DevKeyLoader.loadPrivateKey(
                    "keys/gateway-private.pem");

            String token = Jwts.builder()
                    .subject(serviceName)
                    .claim("type", "service")
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plusSeconds(300)))
                    .signWith(gatewayPrivateKey, Jwts.SIG.RS256)
                    .compact();

            Map<String, String> data = Map.of(
                    "token", token,
                    "serviceName", serviceName,
                    "type", "service",
                    "note", "Service JWT — use for /api/v1/internal/* endpoints"
            );

            log.info("Test service token generated: serviceName={}", serviceName);

            return ResponseEntity.ok(ApiResponse.ok(
                    "Service token generated — dev/test profile only",
                    data,
                    httpRequest.getHeader("X-Request-ID")));

        } catch (Exception ex) {
            log.error("Failed to generate service token: {}", ex.getMessage(), ex);
            throw new RuntimeException(
                    "Failed to generate service token. " +
                            "Check that src/test/resources/keys/gateway-private.pem exists.", ex);
        }
    }
}