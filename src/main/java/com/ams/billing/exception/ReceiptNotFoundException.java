package com.ams.billing.exception;

import org.springframework.http.HttpStatus;

public class ReceiptNotFoundException extends BillingException {
    public ReceiptNotFoundException(String detail) {
        super(detail, HttpStatus.NOT_FOUND, "RECEIPT_NOT_FOUND");
    }
}