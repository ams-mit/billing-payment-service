package com.ams.billing.exception;

import org.springframework.http.HttpStatus;

public class ServiceUnavailableException extends BillingException {
    public ServiceUnavailableException(String serviceName) {
        super("%s is currently unavailable. Please try again later.".formatted(serviceName),
                HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE");
    }
}