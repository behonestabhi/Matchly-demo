package com.matchly.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Body for {@code POST /api/v1/auth/logout}. */
public record LogoutRequest(
        @NotBlank String refreshToken
) {
}
