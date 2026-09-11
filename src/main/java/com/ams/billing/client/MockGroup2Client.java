package com.ams.billing.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Mock implementation of Group2ApiClient for local development and testing.
 *
 * Active ONLY on dev and test profiles.
 * Always returns successful validation — simulates a valid unit with active occupancy.
 *
 * Switch to RealGroup2Client when Group 2 deploys their service.
 * No changes needed in InvoiceServiceImpl — just change the active profile.
 */
@Slf4j
@Component
@Profile({"dev", "test"})
@Primary
public class MockGroup2Client implements Group2ApiClient {

    @Override
    public void validateUnitForInvoicing(String unitId) {
        // Simulate Group 2 returning:
        //   PROP-016: unit exists = true
        //   LEASE-011: active occupancy eligible for billing = true
        log.info("[MOCK] Group 2 validation passed for unitId={} — " +
                 "using mock response (dev/test profile)", unitId);
        // No exception thrown = validation passed
    }
}
