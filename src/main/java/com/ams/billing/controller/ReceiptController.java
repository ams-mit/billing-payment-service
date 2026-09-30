package com.ams.billing.controller;

import com.ams.billing.dto.response.ApiResponse;
import com.ams.billing.dto.response.ReceiptResponse;
import com.ams.billing.service.ReceiptService;
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

@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
@Tag(name = "Receipts", description = "Read-only receipt documents — BILL-021 to BILL-023")
@SecurityRequirement(name = "BearerAuth")
public class ReceiptController {

    private final ReceiptService receiptService;

    // BILL-021
    @Operation(summary = "Get a receipt by ID")
    @GetMapping("/{receiptId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceiptById(
            @PathVariable String receiptId,
            HttpServletRequest httpRequest) {

        ReceiptResponse response = receiptService.getReceiptById(receiptId);

        return ResponseEntity.ok(ApiResponse.ok("Receipt retrieved",
                response, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-022
    @Operation(summary = "Get receipt for a specific payment",
            description = "Returns 404 if payment is still PENDING or was REJECTED.")
    @GetMapping("/payments/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReceiptResponse>> getReceiptByPaymentId(
            @PathVariable String paymentId,
            HttpServletRequest httpRequest) {

        ReceiptResponse response = receiptService.getReceiptByPaymentId(paymentId);

        return ResponseEntity.ok(ApiResponse.ok("Receipt retrieved",
                response, httpRequest.getHeader("X-Request-ID")));
    }

    // BILL-023
    @Operation(summary = "Get all receipts for a unit across all billing periods")
    @GetMapping("/units/{unitId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<ReceiptResponse>>> getReceiptsByUnit(
            @PathVariable String unitId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {

        Page<ReceiptResponse> result = receiptService.getReceiptsByUnit(
                unitId,
                PageRequest.of(page, size, Sort.by("issuedAt").descending()));

        return ResponseEntity.ok(ApiResponse.ok("Unit receipts retrieved",
                result, httpRequest.getHeader("X-Request-ID")));
    }
}