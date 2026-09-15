package com.ams.billing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row in the arrears report — one entry per unit with outstanding balance.
 * Response for GET /api/v1/reports/arrears (BILL-030).
 *
 * Per v2.1: filterable by buildingId, year, month.
 */
@Getter
@Builder
public class ArrearsReportResponse {

    private String unitId;
    private String residentId;
    private BigDecimal outstandingBalance;
    private int unpaidInvoiceCount;
    private int overdueInvoiceCount;
    private Instant oldestUnpaidInvoiceDate;
    private Instant lastPaymentDate;
}