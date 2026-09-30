package com.ams.billing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response for balance endpoints (BILL-026, BILL-027).
 *
 * Balance is ALWAYS computed live from the database.
 * Formula:
 *   outstandingBalance =
 *     SUM(ISSUED + PARTIALLY_PAID invoice amounts)
 *     - SUM(CONFIRMED payment amounts)
 *     - SUM(CREDIT adjustment amounts)
 *     + SUM(DEBIT adjustment amounts)
 *
 * Per v2.1 Section 3.3 — never pre-computed or cached.
 */
@Getter
@Builder
public class BalanceResponse {

    private String unitId;
    private BigDecimal outstandingBalance;
    private int overdueCount;           // number of OVERDUE invoices
    private boolean hasOverdueInvoices; // convenience flag for Group 4
    private Instant lastInvoiceDate;
    private Instant lastPaymentDate;
}