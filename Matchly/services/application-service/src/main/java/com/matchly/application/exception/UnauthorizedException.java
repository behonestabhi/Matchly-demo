package com.matchly.application.exception;

import org.springframework.http.HttpStatus;

/** 401 — no usable caller identity was supplied. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String detail) {
        super(HttpStatus.UNAUTHORIZED, "Unauthorized", detail);
    }
}
