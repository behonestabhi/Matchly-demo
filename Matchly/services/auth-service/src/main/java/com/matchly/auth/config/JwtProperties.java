package com.matchly.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed JWT configuration, bound from the {@code jwt.*} block.
 *
 * @param secret           shared HS256 signing secret (also used by the gateway)
 * @param accessTtlSeconds access-token lifetime in seconds (default 900)
 * @param refreshTtlSeconds refresh-token lifetime in seconds (default 1209600)
 * @param issuer           the {@code iss} claim value
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTtlSeconds,
        long refreshTtlSeconds,
        String issuer
) {
    public JwtProperties {
        if (accessTtlSeconds <= 0) {
            accessTtlSeconds = 900L;
        }
        if (refreshTtlSeconds <= 0) {
            refreshTtlSeconds = 1209600L;
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "matchly-auth";
        }
    }
}
