package com.ams.billing.dto.response;

import com.ams.billing.enums.BillingPeriod;
import com.ams.billing.enums.ChargeType;
import com.ams.billing.entity.ChargeRule;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * What we send back to the client after any charge rule operation.
 *
 * Important: we never send the Entity class directly to the client.
 * The Entity is a database object. The Response is an API object.
 * They look similar but serve different purposes.
 *
 * The static from() method converts an Entity → Response.
 * This keeps conversion logic in one place.
 */
@Getter
@Builder
public class ChargeRuleResponse {

    private String id;
    private String name;
    private ChargeType chargeType;
    private BigDecimal amount;
    private BillingPeriod billingPeriod;
    private boolean applicableToAllUnits;
    private String status;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Converts a ChargeRule entity into a ChargeRuleResponse DTO.
     * Call this in the service layer after saving or loading from the database.
     */
    public static ChargeRuleResponse from(ChargeRule chargeRule) {
        return ChargeRuleResponse.builder()
                .id(chargeRule.getId())
                .name(chargeRule.getName())
                .chargeType(chargeRule.getChargeType())
                .amount(chargeRule.getAmount())
                .billingPeriod(chargeRule.getBillingPeriod())
                .applicableToAllUnits(chargeRule.isApplicableToAllUnits())
                .status(chargeRule.getStatus())
                .createdBy(chargeRule.getCreatedBy())
                .createdAt(chargeRule.getCreatedAt())
                .updatedAt(chargeRule.getUpdatedAt())
                .build();
    }
}