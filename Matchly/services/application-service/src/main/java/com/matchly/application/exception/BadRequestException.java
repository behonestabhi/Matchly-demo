package com.matchly.application.exception;

import org.springframework.http.HttpStatus;

/** 400 — the request was malformed or violates a business rule. */
public class BadRequestException extends ApiException {
    public BadRequestException(String detail) {
        super(HttpStatus.BAD_REQUEST, "Bad request", detail);
    }
}
