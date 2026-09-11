package com.ams.billing.dto.request;

import com.ams.billing.enums.BillingPeriod;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for POST /api/v1/invoices (BILL-007).
 *
 * billingMonth is required for MONTHLY invoices.
 * billingMonth should be null for QUARTERLY invoices.
 * Service layer validates this combination.
 */
@Getter
@Setter
public class CreateInvoiceRequest {

    @NotBlank(message = "Unit ID is required")
    private String unitId;

    @NotBlank(message = "Resident ID is required")
    private String residentId;

    @NotNull(message = "Billing period is required")
    private BillingPeriod billingPeriod;

    @NotNull(message = "Billing year is required")
    @Min(value = 2020, message = "Billing year must be 2020 or later")
    @Max(value = 2100, message = "Billing year must be 2100 or earlier")
    private Integer billingYear;

    // Required for MONTHLY, null for QUARTERLY
    @Min(value = 1, message = "Billing month must be between 1 and 12")
    @Max(value = 12, message = "Billing month must be between 1 and 12")
    private Integer billingMonth;
}