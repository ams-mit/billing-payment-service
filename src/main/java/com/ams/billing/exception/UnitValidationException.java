package com.ams.billing.exception;

import org.springframework.http.HttpStatus;

public class UnitValidationException extends BillingException {
    public UnitValidationException(String unitId) {
        super("Unit %s does not exist or has no active occupancy".formatted(unitId),
                HttpStatus.UNPROCESSABLE_ENTITY, "UNIT_VALIDATION_FAILED");
    }
}