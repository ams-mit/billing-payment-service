package com.ams.billing.controller;

import com.ams.billing.dto.request.CreateAdjustmentRequest;
import com.ams.billing.dto.response.AdjustmentResponse;
import com.ams.billing.dto.response.ApiResponse;
import com.ams.billing.service.AdjustmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/adjustments")
@RequiredArgsConstructor
@Tag(name = "Adjustments",
        description = "Invoice balance adjustments — BILL-024, BILL-025")
@SecurityRequirement(name = "BearerAuth")
public class AdjustmentController {

    private final AdjustmentService adjustmentService;

    // BILL-024
    @Operation(summary = "Apply a CREDIT or DEBIT adjustment to an invoice",
            description = "CREDIT reduces balance. DEBIT increases balance. " +
                    "Reason must be at least 10 characters. " +
                    "Adjustment is immutable after creation. " +
                    "FINANCE_OFFICER only.")
    @PostMapping
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<AdjustmentResponse>> createAdjustment(
            @Valid @RequestBody CreateAdjustmentRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        AdjustmentResponse response = adjustmentService.createAdjustment(
                request, auth.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Adjustment applied successfully",
                        response, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-025
    @Operation(summary = "Get all adjustments for an invoice",
            description = "Shows type, amount, reason, and who applied it.")
    @GetMapping("/invoices/{invoiceId}")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<List<AdjustmentResponse>>> getAdjustmentsByInvoice(
            @PathVariable String invoiceId,
            HttpServletRequest httpRequest) {

        List<AdjustmentResponse> response = adjustmentService
                .getAdjustmentsByInvoice(invoiceId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Adjustments retrieved successfully",
                response, httpRequest.getHeader("X-Request-ID")));
    }
}