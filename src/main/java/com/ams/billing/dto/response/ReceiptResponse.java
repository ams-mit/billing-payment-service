package com.ams.billing.dto.response;

import com.ams.billing.entity.Receipt;
import com.ams.billing.enums.BillingPeriod;
import com.ams.billing.enums.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Response for receipt endpoints (BILL-021, BILL-022, BILL-023).
 *
 * All fields are snapshots — permanently recorded at the time
 * the payment was confirmed. They never change.
 */
@Getter
@Builder
public class ReceiptResponse {

    private String id;
    private String paymentId;
    private String unitId;
    private BillingPeriod billingPeriod;
    private Short billingYear;
    private Byte billingMonth;
    private BigDecimal amountPaid;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private Instant issuedAt;

    public static ReceiptResponse from(Receipt receipt) {
        return ReceiptResponse.builder()
                .id(receipt.getId())
                .paymentId(receipt.getPayment().getId())
                .unitId(receipt.getUnitId())
                .billingPeriod(receipt.getBillingPeriod())
                .billingYear(receipt.getBillingYear())
                .billingMonth(receipt.getBillingMonth())
                .amountPaid(receipt.getAmountPaid())
                .paymentDate(receipt.getPaymentDate())
                .paymentMethod(receipt.getPaymentMethod())
                .referenceNumber(receipt.getReferenceNumber())
                .issuedAt(receipt.getIssuedAt())
                .build();
    }
}