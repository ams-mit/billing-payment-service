package com.ams.billing.controller;

import com.ams.billing.dto.request.CreateInvoiceRequest;
import com.ams.billing.dto.request.UpdateInvoiceStatusRequest;
import com.ams.billing.dto.response.ApiResponse;
import com.ams.billing.dto.response.InvoiceResponse;
import com.ams.billing.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice generation and management — BILL-007 to BILL-013")
@SecurityRequirement(name = "BearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;

    // BILL-007
    @Operation(summary = "Generate invoice for a unit and billing period",
            description = "Validates unit with Group 2, snapshots charge amounts into line items. FINANCE_OFFICER only.")
    @PostMapping
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> generateInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        InvoiceResponse response = invoiceService.generateInvoice(
                request, auth.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Invoice generated successfully",
                        response, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-008
    @Operation(summary = "List all invoices with optional filters")
    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getAllInvoices(
            @RequestParam(required = false) String unitId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        Page<InvoiceResponse> result = invoiceService.getAllInvoices(
                unitId, status, year, month,
                PageRequest.of(page, size, Sort.by("issuedAt").descending()));

        return ResponseEntity.ok(ApiResponse.ok("Invoices retrieved",
                result, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-009
    @Operation(summary = "Get invoice by ID — all authenticated roles")
    @GetMapping("/{invoiceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(
            @PathVariable String invoiceId,
            Authentication auth,
            HttpServletRequest httpRequest) {

        List<String> roles = com.ams.billing.security.SecurityUtils.extractRoles(auth);

        InvoiceResponse response = invoiceService.getInvoiceById(
                invoiceId, auth.getName(), roles);

        return ResponseEntity.ok(ApiResponse.ok("Invoice retrieved",
                response, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-010
    @Operation(summary = "Get all invoices for a unit")
    @GetMapping("/units/{unitId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<InvoiceResponse>>> getInvoicesByUnit(
            @PathVariable String unitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth,
            HttpServletRequest httpRequest) {

        List<String> roles = com.ams.billing.security.SecurityUtils.extractRoles(auth);

        Page<InvoiceResponse> result = invoiceService.getInvoicesByUnit(
                unitId, auth.getName(), roles,
                PageRequest.of(page, size, Sort.by("issuedAt").descending()));

        return ResponseEntity.ok(ApiResponse.ok("Unit invoices retrieved",
                result, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-011
    @Operation(summary = "Get invoice for a unit and billing period")
    @GetMapping("/units/{unitId}/period/{year}/{month}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByUnitAndPeriod(
            @PathVariable String unitId,
            @PathVariable Integer year,
            @PathVariable Integer month,
            Authentication auth,
            HttpServletRequest httpRequest) {

        List<String> roles = com.ams.billing.security.SecurityUtils.extractRoles(auth);

        InvoiceResponse response = invoiceService.getInvoiceByUnitAndPeriod(
                unitId, year, month, auth.getName(), roles);

        return ResponseEntity.ok(ApiResponse.ok("Invoice retrieved",
                response, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-012
    @Operation(summary = "Update invoice status — CANCELLED or OVERDUE",
            description = "CANCELLED requires cancellationReason. Cannot cancel a PAID invoice.")
    @PatchMapping("/{invoiceId}/status")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateInvoiceStatus(
            @PathVariable String invoiceId,
            @Valid @RequestBody UpdateInvoiceStatusRequest request,
            HttpServletRequest httpRequest) {

        InvoiceResponse response = invoiceService.updateInvoiceStatus(invoiceId, request);

        return ResponseEntity.ok(ApiResponse.ok("Invoice status updated",
                response, httpRequest.getHeader("X-Request-ID")));
    }
}