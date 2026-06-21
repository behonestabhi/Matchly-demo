package com.matchly.candidate.exception;

import org.springframework.http.HttpStatus;

/** Raised when a downstream dependency (e.g. matching-service) is unavailable. */
public class ServiceUnavailableException extends ApiException {
    public ServiceUnavailableException(String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable", detail);
    }
}
