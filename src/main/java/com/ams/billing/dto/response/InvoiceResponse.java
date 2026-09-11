package com.ams.billing.dto.response;

import com.ams.billing.entity.Invoice;
import com.ams.billing.enums.BillingPeriod;
import com.ams.billing.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
public class InvoiceResponse {

    private String id;
    private String unitId;
    private String residentId;
    private BillingPeriod billingPeriod;
    private Short billingYear;
    private Byte billingMonth;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private String cancellationReason;
    private String issuedBy;
    private Instant issuedAt;
    private Instant updatedAt;
    private List<InvoiceLineResponse> lines;  // included on single-invoice GET

    public static InvoiceResponse from(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .unitId(invoice.getUnitId())
                .residentId(invoice.getResidentId())
                .billingPeriod(invoice.getBillingPeriod())
                .billingYear(invoice.getBillingYear())
                .billingMonth(invoice.getBillingMonth())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .cancellationReason(invoice.getCancellationReason())
                .issuedBy(invoice.getIssuedBy())
                .issuedAt(invoice.getIssuedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    // Includes line items — used for GET /invoices/{id}
    public static InvoiceResponse fromWithLines(Invoice invoice) {
        InvoiceResponse response = from(invoice);
        List<InvoiceLineResponse> lines = invoice.getLines().stream()
                .map(InvoiceLineResponse::from)
                .toList();
        return InvoiceResponse.builder()
                .id(response.getId())
                .unitId(response.getUnitId())
                .residentId(response.getResidentId())
                .billingPeriod(response.getBillingPeriod())
                .billingYear(response.getBillingYear())
                .billingMonth(response.getBillingMonth())
                .totalAmount(response.getTotalAmount())
                .status(response.getStatus())
                .cancellationReason(response.getCancellationReason())
                .issuedBy(response.getIssuedBy())
                .issuedAt(response.getIssuedAt())
                .updatedAt(response.getUpdatedAt())
                .lines(lines)
                .build();
    }
}