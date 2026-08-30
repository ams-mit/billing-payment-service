package com.ams.billing.exception;

import org.springframework.http.HttpStatus;

public class BillingException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    public BillingException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() { return httpStatus; }
    public String getErrorCode()      { return errorCode; }
}