package com.matchly.job.exception;

import org.springframework.http.HttpStatus;

/** Raised on an illegal state transition (e.g. publishing an already-closed job). */
public class ConflictException extends ApiException {
    public ConflictException(String detail) {
        super(HttpStatus.CONFLICT, "Conflict", detail);
    }
}
