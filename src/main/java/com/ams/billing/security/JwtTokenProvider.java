package com.ams.billing.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/**
 * RS256 JWT provider for billing-payment-service.
 *
 * VERIFY:  All incoming JWTs (user and service) are Gateway JWTs.
 *          We verify them using the Gateway's RSA public key.
 *
 * SIGN:    When making internal calls to other services through the Gateway,
 *          we create a Service JWT signed with our own RSA private key.
 *
 * We NEVER hold the Gateway's private key.
 * We NEVER hold other services' private keys.
 *
 * In local dev/test: RSA keys are generated once and stored in
 * src/test/resources/keys/ — never committed as real production keys.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final RSAPublicKey gatewayPublicKey;
    private final RSAPrivateKey servicePrivateKey;

    @Value("${app.jwt.service-name}")
    private String serviceName;

    @Value("${app.jwt.service-jwt-expiry-ms}")
    private long serviceJwtExpiryMs;

    public JwtTokenProvider(
            @Value("${app.jwt.gateway-public-key}") String gatewayPublicKeyPem,
            @Value("${app.jwt.service-private-key}") String servicePrivateKeyPem) {

        this.gatewayPublicKey  = parsePublicKey(gatewayPublicKeyPem);
        this.servicePrivateKey = parsePrivateKey(servicePrivateKeyPem);
    }

    // ── Verification ──────────────────────────────────────────

    /**
     * Validates an incoming Gateway JWT (user or service type).
     * Checks RS256 signature, expiry, and required claims.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(gatewayPublicKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("Gateway JWT expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException | MalformedJwtException ex) {
            log.warn("Gateway JWT invalid format: {}", ex.getMessage());
        } catch (SignatureException ex) {
            log.warn("Gateway JWT RS256 signature invalid — possible key mismatch");
        } catch (IllegalArgumentException ex) {
            log.warn("Gateway JWT claims empty or token malformed: {}. Token starts with: {}",
                    ex.getMessage(),
                    token != null && token.length() > 10 ? token.substring(0, 10) : "empty");
        }
        return false;
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(gatewayPublicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts roles from the JWT.
     * User JWTs have roles: ["FINANCE_OFFICER", "TENANT", etc.]
     * Service JWTs have no roles claim.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = extractAllClaims(token).get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    /**
     * Extracts the type claim from the JWT.
     * Returns "user" for user requests or "service" for internal service calls.
     */
    public String extractTokenType(String token) {
        Object type = extractAllClaims(token).get("type");
        return type != null ? type.toString() : "";
    }

    // ── Service JWT creation ──────────────────────────────────

    /**
     * Creates a Service JWT signed with this service's RSA private key.
     * Used when billing-payment-service needs to call another service
     * through the API Gateway.
     *
     * Lifetime: 5 minutes per v2.1 spec.
     */
    public String createServiceJwt() {
        return Jwts.builder()
                .subject(serviceName)
                .claim("type", "service")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + serviceJwtExpiryMs))
                .signWith(servicePrivateKey, Jwts.SIG.RS256)
                .compact();
    }

    // ── Key parsing helpers ───────────────────────────────────

    private RSAPublicKey parsePublicKey(String pem) {
        try {
            String stripped = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(stripped);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception ex) {
            log.warn("Gateway public key not configured — JWT validation will fail. " +
                    "Set GATEWAY_JWT_PUBLIC_KEY env var or use dev key setup.");
            return null;
        }
    }

    private RSAPrivateKey parsePrivateKey(String pem) {
        try {
            String stripped = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(stripped);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception ex) {
            log.warn("Service private key not configured — outgoing service calls will fail. " +
                    "Set SERVICE_JWT_PRIVATE_KEY env var or use dev key setup.");
            return null;
        }
    }
}