package com.ams.billing.dto.request;

import com.ams.billing.enums.BillingPeriod;
import com.ams.billing.enums.ChargeType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * What the Finance Officer sends when creating a new charge rule.
 *
 * @Valid in the controller triggers all the annotations below.
 * If any field fails validation, Spring returns 400 Bad Request
 * automatically — you never reach the service layer.
 */
@Getter
@Setter
public class CreateChargeRuleRequest {

    @NotBlank(message = "Charge rule name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotNull(message = "Charge type is required")
    private ChargeType chargeType;
    // valid values: MANAGEMENT_FEE, PARKING_FEE, FACILITY_FEE, UTILITY

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "Billing period is required")
    private BillingPeriod billingPeriod;
    // valid values: MONTHLY, QUARTERLY

    private boolean applicableToAllUnits = true;
}