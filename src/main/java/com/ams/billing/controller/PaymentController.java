package com.ams.billing.controller;

import com.ams.billing.dto.request.RecordPaymentRequest;
import com.ams.billing.dto.request.UpdatePaymentStatusRequest;
import com.ams.billing.dto.response.ApiResponse;
import com.ams.billing.dto.response.PaymentResponse;
import com.ams.billing.enums.PaymentStatus;
import com.ams.billing.service.PaymentService;
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
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment recording and management — BILL-014 to BILL-020")
@SecurityRequirement(name = "BearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    // BILL-014
    @Operation(summary = "Record a simulated payment against an invoice",
            description = "Creates a PENDING payment. Must be CONFIRMED via PATCH /status. " +
                    "Validates no overpayment. FINANCE_OFFICER only.")
    @PostMapping
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @Valid @RequestBody RecordPaymentRequest request,
            Authentication auth,
            HttpServletRequest httpRequest) {

        PaymentResponse response = paymentService.recordPayment(
                request, auth.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Payment recorded successfully",
                        response, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-015
    @Operation(summary = "List all payments with optional filters")
    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'APARTMENT_MANAGER')")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(
            @RequestParam(required = false) String invoiceId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        Page<PaymentResponse> result = paymentService.getAllPayments(
                invoiceId, status,
                PageRequest.of(page, size, Sort.by("recordedAt").descending()));

        return ResponseEntity.ok(ApiResponse.ok("Payments retrieved",
                result, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-016
    @Operation(summary = "Get payment by ID")
    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable String paymentId,
            Authentication auth,
            HttpServletRequest httpRequest) {

        List<String> roles = extractRoles(auth);
        PaymentResponse response = paymentService.getPaymentById(
                paymentId, auth.getName(), roles);

        return ResponseEntity.ok(ApiResponse.ok("Payment retrieved",
                response, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-017
    @Operation(summary = "Get all payments for an invoice")
    @GetMapping("/invoices/{invoiceId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPaymentsByInvoice(
            @PathVariable String invoiceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth,
            HttpServletRequest httpRequest) {

        List<String> roles = extractRoles(auth);
        Page<PaymentResponse> result = paymentService.getPaymentsByInvoice(
                invoiceId, auth.getName(), roles,
                PageRequest.of(page, size, Sort.by("recordedAt").descending()));

        return ResponseEntity.ok(ApiResponse.ok("Payments retrieved",
                result, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-018
    @Operation(summary = "Get payment history for a unit")
    @GetMapping("/units/{unitId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPaymentsByUnit(
            @PathVariable String unitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth,
            HttpServletRequest httpRequest) {

        List<String> roles = extractRoles(auth);
        Page<PaymentResponse> result = paymentService.getPaymentsByUnit(
                unitId, auth.getName(), roles,
                PageRequest.of(page, size, Sort.by("recordedAt").descending()));

        return ResponseEntity.ok(ApiResponse.ok("Unit payments retrieved",
                result, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-019
    @Operation(summary = "Get all payments for a resident")
    @GetMapping("/residents/{residentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPaymentsByResident(
            @PathVariable String residentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth,
            HttpServletRequest httpRequest) {

        List<String> roles = extractRoles(auth);
        Page<PaymentResponse> result = paymentService.getPaymentsByResident(
                residentId, auth.getName(), roles,
                PageRequest.of(page, size, Sort.by("recordedAt").descending()));

        return ResponseEntity.ok(ApiResponse.ok("Resident payments retrieved",
                result, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-020
    @Operation(summary = "Confirm or reject a payment",
            description = "CONFIRMED triggers receipt generation and invoice status update. " +
                    "REJECTED requires rejectionReason. FINANCE_OFFICER only.")
    @PatchMapping("/{paymentId}/status")
    @PreAuthorize("hasRole('FINANCE_OFFICER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> updatePaymentStatus(
            @PathVariable String paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequest request,
            HttpServletRequest httpRequest) {

        PaymentResponse response = paymentService.updatePaymentStatus(
                paymentId, request);

        return ResponseEntity.ok(ApiResponse.ok("Payment status updated",
                response, httpRequest.getHeader("X-Request-ID")));
    }

    private List<String> extractRoles(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(r -> r.replace("ROLE_", ""))
                .toList();
    }
}