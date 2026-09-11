package com.ams.billing.controller;

import com.ams.billing.dto.response.ApiResponse;
import com.ams.billing.dto.response.InvoiceLineResponse;
import com.ams.billing.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoice Lines", description = "Read-only invoice line items — BILL-013")
@SecurityRequirement(name = "BearerAuth")
public class InvoiceLineController {

    private final InvoiceService invoiceService;

    // BILL-013
    @Operation(summary = "Get all line items for an invoice",
            description = "Line item amounts are permanent snapshots from invoice generation time. " +
                    "They never change even if the parent charge rule is updated.")
    @GetMapping("/{invoiceId}/lines")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<InvoiceLineResponse>>> getInvoiceLines(
            @PathVariable String invoiceId,
            Authentication auth,
            HttpServletRequest httpRequest) {

        List<String> roles = com.ams.billing.security.SecurityUtils.extractRoles(auth);

        List<InvoiceLineResponse> lines = invoiceService.getInvoiceLines(
                invoiceId, auth.getName(), roles);

        return ResponseEntity.ok(ApiResponse.ok(
                "Invoice lines retrieved successfully",
                lines,
                httpRequest.getHeader("X-Request-ID")));
    }
}