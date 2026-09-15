package com.ams.billing.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * One row per month in the collection summary.
 * Response for GET /api/v1/reports/collection-summary (BILL-031).
 *
 * Query param: year — returns one row per month for that year.
 */
@Getter
@Builder
public class CollectionSummaryResponse {

    private int year;
    private int month;
    private String monthLabel;           // e.g. "September 2026"
    private BigDecimal totalBilled;      // sum of invoice totals issued this month
    private BigDecimal totalCollected;   // sum of CONFIRMED payments this month
    private BigDecimal totalOutstanding; // billed - collected
    private double collectionRatePercent;
    private int invoiceCount;
    private int paymentCount;
}