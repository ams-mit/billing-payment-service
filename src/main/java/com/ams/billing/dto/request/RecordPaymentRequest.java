package com.ams.billing.dto.request;

import com.ams.billing.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for POST /api/v1/payments (BILL-014).
 *
 * This records a SIMULATED payment — no real payment gateway.
 * Finance Officer manually enters details after receiving
 * offline confirmation (bank slip, cash receipt, etc).
 *
 * The payment starts as PENDING until explicitly CONFIRMED
 * via PATCH /payments/{id}/status (BILL-020).
 */
@Getter
@Setter
public class RecordPaymentRequest {

    @NotBlank(message = "Invoice ID is required")
    private String invoiceId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Payment amount must be greater than zero")
    @Digits(integer = 10, fraction = 2,
            message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Payment date is required")
    @PastOrPresent(message = "Payment date cannot be in the future")
    private LocalDate paymentDate;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "Reference number is required")
    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    private String referenceNumber;
}