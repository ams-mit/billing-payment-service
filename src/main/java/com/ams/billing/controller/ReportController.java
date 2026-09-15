package com.ams.billing.controller;

import com.ams.billing.dto.response.*;
import com.ams.billing.enums.PaymentMethod;
import com.ams.billing.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports",
        description = "Financial reports and dashboard — BILL-029 to BILL-032")
@SecurityRequirement(name = "BearerAuth")
public class ReportController {

    private final ReportService reportService;

    // BILL-029
    @Operation(summary = "Finance dashboard summary",
            description = "Returns current month totals: invoiced, collected, " +
                    "outstanding, overdue count, collection rate.")
    @GetMapping("/finance-dashboard")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<FinanceDashboardResponse>> getFinanceDashboard(
            HttpServletRequest httpRequest) {

        FinanceDashboardResponse response = reportService.getFinanceDashboard();

        return ResponseEntity.ok(ApiResponse.ok(
                "Finance dashboard retrieved successfully",
                response, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-030
    @Operation(summary = "Arrears report",
            description = "All units with unpaid or overdue invoices. " +
                    "Filterable by buildingId, year, month.")
    @GetMapping("/arrears")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<Page<ArrearsReportResponse>>> getArrearsReport(
            @RequestParam(required = false) String buildingId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        Page<ArrearsReportResponse> response = reportService.getArrearsReport(
                buildingId, year, month,
                PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.ok(
                "Arrears report retrieved successfully",
                response, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-031
    @Operation(summary = "Monthly collection summary",
            description = "Total billed vs collected per month for a given year. " +
                    "Returns 12 rows — one per month.")
    @GetMapping("/collection-summary")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<List<CollectionSummaryResponse>>> getCollectionSummary(
            @RequestParam int year,
            HttpServletRequest httpRequest) {

        List<CollectionSummaryResponse> response =
                reportService.getCollectionSummary(year);

        return ResponseEntity.ok(ApiResponse.ok(
                "Collection summary retrieved successfully",
                response, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-032
    @Operation(summary = "Full payment history for audit and reconciliation",
            description = "All payments across all units. " +
                    "Filterable by year, month, paymentMethod.")
    @GetMapping("/payment-history")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<Page<PaymentHistoryResponse>>> getPaymentHistory(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        Page<PaymentHistoryResponse> response = reportService.getPaymentHistory(
                year, month, paymentMethod,
                PageRequest.of(page, size,
                        Sort.by("recordedAt").descending()));

        return ResponseEntity.ok(ApiResponse.ok(
                "Payment history retrieved successfully",
                response, httpRequest.getHeader("X-Request-ID")));
    }
}