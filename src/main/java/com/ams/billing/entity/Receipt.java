package com.ams.billing.entity;

import com.ams.billing.enums.BillingPeriod;
import com.ams.billing.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "receipts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Receipt extends BaseEntity {

    // One-to-one with Payment — receipt is generated per confirmed payment
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, updatable = false, unique = true)
    private Payment payment;

    // Snapshot fields — copied from invoice and payment at receipt generation time.
    // Receipt must remain readable even if parent records change state later.
    @Column(name = "unit_id", nullable = false, length = 36, updatable = false)
    private String unitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false, length = 20, updatable = false)
    private BillingPeriod billingPeriod;

    @Column(name = "billing_year", nullable = false, updatable = false)
    private Short billingYear;

    @Column(name = "billing_month", updatable = false)
    private Byte billingMonth;          // null for QUARTERLY receipts

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amountPaid;

    @Column(name = "payment_date", nullable = false, updatable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30, updatable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "reference_number", nullable = false, length = 100, updatable = false)
    private String referenceNumber;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Override
    protected void onPrePersist() {
        if (this.issuedAt == null) {
            this.issuedAt = Instant.now();
        }
    }
}