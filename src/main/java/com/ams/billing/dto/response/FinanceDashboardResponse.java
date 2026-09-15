package com.ams.billing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Response for GET /api/v1/reports/finance-dashboard (BILL-029).
 *
 * Per v2.1: returns current month aggregates.
 * totalInvoicedThisMonth — sum of all invoice totals issued this month
 * totalCollectedThisMonth — sum of all CONFIRMED payments this month
 * totalOutstanding — sum of all unpaid balances across all time
 * overdueAccountsCount — number of units with OVERDUE invoices
 * collectionRatePercent — (collected / invoiced) * 100 for this month
 */
@Getter
@Builder
public class FinanceDashboardResponse {

    private BigDecimal totalInvoicedThisMonth;
    private BigDecimal totalCollectedThisMonth;
    private BigDecimal totalOutstanding;
    private int overdueAccountsCount;
    private double collectionRatePercent;
    private int totalActiveInvoices;
    private int totalPaidInvoicesThisMonth;
    private String reportMonth;   // e.g. "2026-09"
}