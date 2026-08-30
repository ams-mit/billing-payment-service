package com.ams.billing.entity;

import com.ams.billing.enums.BillingPeriod;
import com.ams.billing.enums.ChargeType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "charge_rules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChargeRule extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", nullable = false, length = 50)
    private ChargeType chargeType;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_period", nullable = false, length = 20)
    private BillingPeriod billingPeriod;

    @Column(name = "applicable_to_all_units", nullable = false)
    private boolean applicableToAllUnits = true;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;
}