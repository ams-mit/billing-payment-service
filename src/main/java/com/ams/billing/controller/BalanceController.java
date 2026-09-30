package com.ams.billing.controller;

import com.ams.billing.dto.response.ApiResponse;
import com.ams.billing.dto.response.BalanceResponse;
import com.ams.billing.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/balance")
@RequiredArgsConstructor
@Tag(name = "Balance", description = "Live balance calculation — BILL-026 to BILL-027")
@SecurityRequirement(name = "BearerAuth")
public class BalanceController {

    private final BalanceService balanceService;

    // BILL-026
    @Operation(summary = "Get outstanding balance for a unit",
            description = "Always computed live. Never cached. " +
                    "Includes overdueCount and hasOverdueInvoices for Group 4 compatibility.")
    @GetMapping("/units/{unitId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalanceByUnit(
            @PathVariable String unitId,
            HttpServletRequest httpRequest) {

        BalanceResponse response = balanceService.getBalanceByUnit(unitId);

        return ResponseEntity.ok(ApiResponse.ok("Balance retrieved",
                response, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-027
    @Operation(summary = "Get outstanding balance for a resident",
            description = "Supports residents with invoices across multiple units.")
    @GetMapping("/residents/{residentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalanceByResident(
            @PathVariable String residentId,
            HttpServletRequest httpRequest) {

        BalanceResponse response = balanceService.getBalanceByResident(residentId);

        return ResponseEntity.ok(ApiResponse.ok("Balance retrieved",
                response, httpRequest.getHeader("X-Request-ID")));
    }
}