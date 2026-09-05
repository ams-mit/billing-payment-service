package com.ams.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Used for all PATCH /status endpoints across the service.
 * For charge rules: ACTIVE or INACTIVE.
 * For invoices: CANCELLED or OVERDUE (different endpoints reuse this same class).
 *
 * @Pattern ensures only allowed values are accepted.
 * This is validated before reaching the controller method body.
 */
@Getter
@Setter
public class UpdateStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(
            regexp = "ACTIVE|INACTIVE|CANCELLED|OVERDUE",
            message = "Status must be one of: ACTIVE, INACTIVE, CANCELLED, OVERDUE"
    )
    private String status;

    // Optional reason — required when cancelling an invoice (enforced in service layer)
    private String reason;
}