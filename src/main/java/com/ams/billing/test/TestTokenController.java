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
 * Only active when spring.profiles.active=dev or test.
 * Remove or disable before production deployment.
 */
@Hidden                          // hidden from Swagger UI
@Slf4j
@RestController
@RequestMapping("/test/token")
@Profile({"dev", "test"})        // never runs in prod profile
public class TestTokenController {

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> generateToken(
            @RequestParam String userId,
            @RequestParam String role,
            HttpServletRequest request) throws Exception {

        // Load the local dev gateway private key
        // In real system the Gateway signs these — we simulate it locally
        RSAPrivateKey gatewayPrivateKey = DevKeyLoader.loadPrivateKey(
                "src/test/resources/keys/gateway-private.pem");

        String token = Jwts.builder()
                .subject(userId)
                .claim("type", "user")
                .claim("roles", List.of(role))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(86400)))
                .signWith(gatewayPrivateKey, Jwts.SIG.RS256)   // RS256 matching v2.1
                .compact();

        Map<String, String> data = Map.of(
                "token", token,
                "userId", userId,
                "role", role,
                "note", "Signed with local dev gateway key. RS256. Dev profile only."
        );

        return ResponseEntity.ok(
                ApiResponse.ok("Test token generated", data,
                        request.getHeader("X-Request-ID")));
    }

    /**
     * Generates a Service JWT for testing internal endpoints.
     * Simulates what Group 4's community-service would send.
     * Dev/test profile only.
     */
    @PostMapping("/service-token")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateServiceToken(
            @RequestParam String serviceName,
            HttpServletRequest request) throws Exception {

        RSAPrivateKey gatewayPrivateKey = DevKeyLoader.loadPrivateKey(
                "src/test/resources/keys/gateway-private.pem");

        String token = Jwts.builder()
                .subject(serviceName)
                .claim("type", "service")   // type = service per v2.1
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(300))) // 5 min
                .signWith(gatewayPrivateKey, Jwts.SIG.RS256)
                .compact();

        Map<String, String> data = Map.of(
                "token", token,
                "serviceName", serviceName,
                "type", "service",
                "note", "Service JWT — use for /api/v1/internal/* endpoints"
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Service token generated", data,
                request.getHeader("X-Request-ID")));
    }

}
