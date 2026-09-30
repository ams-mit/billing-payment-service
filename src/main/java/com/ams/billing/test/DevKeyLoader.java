package com.ams.billing.test;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads RSA keys from the filesystem for local dev testing.
 * Used ONLY by TestTokenController — never in production code.
 *
 * Keys are located in: src/test/resources/keys/
 *   gateway-private.pem  — signs test tokens (simulates the real API Gateway)
 *   gateway-public.pem   — loaded by JwtTokenProvider to verify tokens
 *   service-private.pem  — this service's own private key
 *   service-public.pem   — this service's public key (shared with Gateway)
 */
@Slf4j
public class DevKeyLoader {

    public static RSAPrivateKey loadPrivateKey(String pemPath) throws Exception {
        String pem = readPemFile(pemPath);

        // Strip PKCS8 headers
        String stripped = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(stripped);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    }

    public static RSAPublicKey loadPublicKey(String pemPath) throws Exception {
        String pem = readPemFile(pemPath);

        String stripped = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(stripped);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(decoded));
    }

    private static String readPemFile(String pemPath) throws IOException {
        // 1. Try as absolute/relative filesystem path
        Path path = Path.of(pemPath);
        if (Files.exists(path)) {
            return Files.readString(path);
        }

        // 2. Try as classpath resource
        try (var is = DevKeyLoader.class.getClassLoader().getResourceAsStream(pemPath)) {
            if (is != null) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.debug("Classpath load failed for {}: {}", pemPath, e.getMessage());
        }

        // 3. If all fail, throw the original detailed error
        throw new IOException(
                "RSA key file not found: " + pemPath + " (checked filesystem and classpath)\n" +
                        "Run these commands to generate dev keys:\n" +
                        "  mkdir -p src/main/resources/keys\n" +
                        "  openssl genrsa -out src/main/resources/keys/gateway-private.pem 2048\n" +
                        "  openssl rsa -in src/main/resources/keys/gateway-private.pem " +
                        "-pubout -out src/main/resources/keys/gateway-public.pem\n" +
                        "  openssl genrsa -out src/main/resources/keys/service-private.pem 2048\n" +
                        "  openssl rsa -in src/main/resources/keys/service-private.pem " +
                        "-pubout -out src/main/resources/keys/service-public.pem"
        );
    }
}