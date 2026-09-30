package com.ams.billing.exception;

import org.springframework.http.HttpStatus;

public class ChargeRuleNotFoundException extends BillingException {
    public ChargeRuleNotFoundException(String chargeRuleId) {
        super("Charge rule not found: " + chargeRuleId,
                HttpStatus.NOT_FOUND,
                "CHARGE_RULE_NOT_FOUND");
    }
}