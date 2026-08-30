package com.ams.billing.entity;

import com.ams.billing.enums.ChargeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoice_lines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoiceLine {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private Invoice invoice;

    // NOT a FK — deliberately stored as plain String (snapshot pattern)
    @Column(name = "charge_rule_id", nullable = false, length = 36, updatable = false)
    private String chargeRuleId;

    @Column(name = "charge_rule_name", nullable = false, length = 100, updatable = false)
    private String chargeRuleName;    // snapshot

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 50, updatable = false)
    private ChargeType chargeType;    // snapshot

    @Column(name = "amount", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;        // snapshot — immutable after creation

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void prePersist() {
        if (this.id == null) {
            this.id = java.util.UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}