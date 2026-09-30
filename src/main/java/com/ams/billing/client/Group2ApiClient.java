package com.ams.billing.client;

/**
 * Abstraction over Group 2's APIs consumed during invoice generation.
 *
 * Two implementations exist:
 *   MockGroup2Client     — active on dev/test profiles. No HTTP calls.
 *   RealGroup2Client     — active on prod profile. Calls real Group 2 endpoints.
 *
 * InvoiceServiceImpl depends only on this interface.
 * Switching from mock to real requires no change in InvoiceServiceImpl.
 */
public interface Group2ApiClient {

    /**
     * Validates that a unit exists (PROP-016) AND has an active occupancy
     * eligible for billing (LEASE-011).
     *
     * Both checks must pass before invoice generation can proceed.
     *
     * @param unitId the unit UUID to validate
     * @throws com.ams.billing.exception.UnitValidationException if validation fails
     * @throws com.ams.billing.exception.ServiceUnavailableException if Group 2 is unreachable
     */
    void validateUnitForInvoicing(String unitId);
}
