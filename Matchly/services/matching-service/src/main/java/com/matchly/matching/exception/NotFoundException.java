package com.matchly.matching.exception;

import org.springframework.http.HttpStatus;

/** 404 — the requested resource does not exist. */
public class NotFoundException extends ApiException {
    public NotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, "Not found", detail);
    }
}
