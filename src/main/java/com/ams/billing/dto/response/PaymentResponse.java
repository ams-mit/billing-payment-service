package com.ams.billing.dto.response;

import com.ams.billing.entity.Payment;
import com.ams.billing.enums.PaymentMethod;
import com.ams.billing.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Builder
public class PaymentResponse {

    private String id;
    private String invoiceId;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private PaymentStatus status;
    private String rejectionReason;
    private String recordedBy;
    private Instant recordedAt;
    private Instant updatedAt;

    // Included when payment is CONFIRMED
    private String receiptId;

    public static PaymentResponse from(Payment payment) {
        PaymentResponse.PaymentResponseBuilder builder = PaymentResponse.builder()
                .id(payment.getId())
                .invoiceId(payment.getInvoice().getId())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .referenceNumber(payment.getReferenceNumber())
                .status(payment.getStatus())
                .rejectionReason(payment.getRejectionReason())
                .recordedBy(payment.getRecordedBy())
                .recordedAt(payment.getRecordedAt())
                .updatedAt(payment.getUpdatedAt());

        // Include receiptId if receipt exists
        if (payment.getReceipt() != null) {
            builder.receiptId(payment.getReceipt().getId());
        }

        return builder.build();
    }
}