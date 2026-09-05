package com.ams.billing.exception;

import org.springframework.http.HttpStatus;

public class DuplicateChargeRuleException extends BillingException {
    public DuplicateChargeRuleException(String name) {
        super("An active charge rule named '%s' already exists".formatted(name),
                HttpStatus.CONFLICT,
                "DUPLICATE_CHARGE_RULE");
    }
}