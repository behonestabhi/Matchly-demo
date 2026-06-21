package com.matchly.application.exception;

import org.springframework.http.HttpStatus;

/** 409 — the request conflicts with current state (e.g. duplicate application). */
public class ConflictException extends ApiException {
    public ConflictException(String detail) {
        super(HttpStatus.CONFLICT, "Conflict", detail);
    }
}
