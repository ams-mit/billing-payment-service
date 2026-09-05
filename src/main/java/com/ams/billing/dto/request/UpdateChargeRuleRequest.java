package com.ams.billing.dto.request;

import com.ams.billing.enums.BillingPeriod;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * What the Finance Officer sends when updating an existing charge rule.
 * Note: chargeType is NOT updatable — changing the type of an existing
 * rule would break audit trails. Create a new rule instead.
 */
@Getter
@Setter
public class UpdateChargeRuleRequest {

    @NotBlank(message = "Charge rule name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Billing period is required")
    private BillingPeriod billingPeriod;

    private boolean applicableToAllUnits;
}