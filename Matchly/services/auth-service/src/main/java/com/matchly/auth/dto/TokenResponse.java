package com.matchly.auth.dto;

/**
 * Response for login/refresh.
 *
 * <pre>{ "accessToken":"eyJ...", "refreshToken":"...", "expiresIn":900, "tokenType":"Bearer" }</pre>
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType
) {
    public static TokenResponse bearer(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, expiresIn, "Bearer");
    }
}
