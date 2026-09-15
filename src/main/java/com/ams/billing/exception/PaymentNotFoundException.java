package com.ams.billing.exception;

import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends BillingException {
    public PaymentNotFoundException(String paymentId) {
        super("Payment not found: " + paymentId,
                HttpStatus.NOT_FOUND,
                "PAYMENT_NOT_FOUND");
    }
}