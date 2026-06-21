package com.matchly.matching.exception;

import org.springframework.http.HttpStatus;

/** 403 — the caller lacks the required role/permission. */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String detail) {
        super(HttpStatus.FORBIDDEN, "Forbidden", detail);
    }
}
