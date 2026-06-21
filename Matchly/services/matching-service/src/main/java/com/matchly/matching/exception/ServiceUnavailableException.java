package com.matchly.matching.exception;

import org.springframework.http.HttpStatus;

/**
 * 503 — a downstream dependency (candidate / job / AI service) was unavailable,
 * so a score could not be computed on demand. Honors the fail-soft principle:
 * reads of already-persisted data still succeed; only on-demand compute degrades.
 */
public class ServiceUnavailableException extends ApiException {
    public ServiceUnavailableException(String detail) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Score unavailable", detail);
    }
}
