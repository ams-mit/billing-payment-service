package com.ams.billing.exception;

import org.springframework.http.HttpStatus;

public class InvoiceNotFoundException extends BillingException {
    public InvoiceNotFoundException(String invoiceId) {
        super("Invoice not found: " + invoiceId, HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND");
    }
}