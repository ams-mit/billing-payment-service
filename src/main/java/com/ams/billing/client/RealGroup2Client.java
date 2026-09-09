package com.ams.billing.client;

import com.ams.billing.exception.ServiceUnavailableException;
import com.ams.billing.exception.UnitValidationException;
import com.ams.billing.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * Real HTTP implementation of Group2ApiClient.
 * Active ONLY on prod profile.
 *
 * Calls two Group 2 endpoints through the API Gateway:
 *   PROP-016: GET /api/v1/internal/units/{unitId}/exists
 *   LEASE-011: GET /api/v1/internal/occupancies/active-billing?unitId={unitId}
 *
 * Each call uses a fresh Service JWT signed with this service's RSA private key.
 * The Gateway verifies the Service JWT and forwards a Gateway Service JWT.
 *
 * TODO: Activate and test when Group 2 deploys their service.
 *       Change spring.profiles.active from dev to prod.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class RealGroup2Client implements Group2ApiClient {

    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    @Value("${app.services.api-gateway-url}")
    private String apiGatewayUrl;

    @Override
    public void validateUnitForInvoicing(String unitId) {
        validateUnitExists(unitId);
        validateActiveOccupancy(unitId);
    }

    private void validateUnitExists(String unitId) {
        // PROP-016: GET /api/v1/internal/units/{unitId}/exists
        String url = apiGatewayUrl + "/api/v1/internal/units/" + unitId + "/exists";
        log.info("Calling PROP-016 to validate unit: unitId={}", unitId);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    buildServiceJwtRequest(),
                    Void.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new UnitValidationException(unitId);
            }

        } catch (HttpClientErrorException.NotFound ex) {
            throw new UnitValidationException(unitId);
        } catch (ResourceAccessException ex) {
            log.error("PROP-016 unreachable: {}", ex.getMessage());
            throw new ServiceUnavailableException("property-unit-service (Group 2)");
        }
    }

    private void validateActiveOccupancy(String unitId) {
        // LEASE-011: GET /api/v1/internal/occupancies/active-billing?unitId={unitId}
        String url = apiGatewayUrl
                + "/api/v1/internal/occupancies/active-billing?unitId=" + unitId;
        log.info("Calling LEASE-011 to validate occupancy: unitId={}", unitId);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    buildServiceJwtRequest(),
                    Void.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new UnitValidationException(unitId);
            }

        } catch (HttpClientErrorException.NotFound ex) {
            throw new UnitValidationException(unitId);
        } catch (ResourceAccessException ex) {
            log.error("LEASE-011 unreachable: {}", ex.getMessage());
            throw new ServiceUnavailableException("lease-occupancy-service (Group 2)");
        }
    }

    /**
     * Creates an HTTP entity with Authorization: Bearer <SERVICE_JWT>.
     * The Service JWT is signed with this service's RSA private key.
     * Per v2.1: this is sent to the Gateway — never directly to Group 2.
     */
    private HttpEntity<Void> buildServiceJwtRequest() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtTokenProvider.createServiceJwt());
        return new HttpEntity<>(headers);
    }
}
