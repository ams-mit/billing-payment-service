package com.ams.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for PATCH /api/v1/invoices/{invoiceId}/status (BILL-012).
 *
 * Only FINANCE_OFFICER can change invoice status.
 * CANCELLED requires a cancellationReason.
 * A PAID invoice cannot be cancelled.
 */
@Getter
@Setter
public class UpdateInvoiceStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
            regexp = "CANCELLED|OVERDUE",
            message = "Invoice status can only be set to CANCELLED or OVERDUE"
    )
    private String status;

    // Required when status = CANCELLED, optional for OVERDUE
    private String cancellationReason;
}