package com.ams.billing.exception;

import org.springframework.http.HttpStatus;

public class OverpaymentException extends BillingException {
    public OverpaymentException() {
        super("Payment amount exceeds the outstanding invoice balance",
                HttpStatus.UNPROCESSABLE_ENTITY, "OVERPAYMENT_NOT_ALLOWED");
    }
}