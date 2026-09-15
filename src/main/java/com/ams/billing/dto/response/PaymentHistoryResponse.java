package com.ams.billing.dto.response;

import com.ams.billing.enums.PaymentMethod;
import com.ams.billing.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One row per payment in the full payment history audit report.
 * Response for GET /api/v1/reports/payment-history (BILL-032).
 *
 * Filterable by year, month, paymentMethod.
 * Used for audit and reconciliation by Finance Officers.
 */
@Getter
@Builder
public class PaymentHistoryResponse {

    private String paymentId;
    private String invoiceId;
    private String unitId;
    private String residentId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private PaymentStatus status;
    private String recordedBy;
    private Instant recordedAt;
    private String receiptId;     // null if not confirmed
}