package com.ams.billing.exception;

import org.springframework.http.HttpStatus;

public class DuplicateInvoiceException extends BillingException {
    public DuplicateInvoiceException(String unitId, int year, int month) {
        super("Active invoice already exists for unit %s, period %d/%d".formatted(unitId, month, year),
                HttpStatus.CONFLICT, "DUPLICATE_INVOICE");
    }
}