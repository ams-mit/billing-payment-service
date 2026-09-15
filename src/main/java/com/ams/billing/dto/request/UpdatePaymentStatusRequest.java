package com.ams.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Request body for PATCH /api/v1/payments/{paymentId}/status (BILL-020).
 *
 * CONFIRMED → triggers receipt generation + invoice status recalculation
 * REJECTED  → requires rejectionReason, no receipt generated
 *
 * Rejection reason is validated in the service layer
 * (required when status = REJECTED, not needed for CONFIRMED).
 */
@Getter
@Setter
public class UpdatePaymentStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
            regexp = "CONFIRMED|REJECTED",
            message = "Payment status must be CONFIRMED or REJECTED"
    )
    private String status;

    // Required when status = REJECTED
    // Validated in service layer — not here — because
    // the requirement depends on the status value
    private String rejectionReason;
}