package com.ams.billing.dto.request;

import com.ams.billing.enums.AdjustmentType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request body for POST /api/v1/adjustments (BILL-024).
 *
 * CREDIT — reduces the outstanding balance (discount, correction)
 * DEBIT  — increases the outstanding balance (penalty, missed charge)
 *
 * reason is mandatory and must be at least 10 characters.
 * This is enforced at both application and database level.
 */
@Getter
@Setter
public class CreateAdjustmentRequest {

    @NotBlank(message = "Invoice ID is required")
    private String invoiceId;

    @NotNull(message = "Adjustment type is required")
    private AdjustmentType adjustmentType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Adjustment amount must be greater than zero")
    @Digits(integer = 10, fraction = 2,
            message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Reason is required")
    @Size(min = 10, max = 1000,
            message = "Reason must be between 10 and 1000 characters")
    private String reason;
}