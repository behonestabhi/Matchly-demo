package com.matchly.job.security;

import java.util.List;
import java.util.UUID;

/**
 * Immutable identity of the calling user, established by {@link HeaderAuthFilter}
 * from the gateway-forwarded {@code X-User-Id} / {@code X-User-Roles} headers and
 * stashed as a request attribute. Services trust these headers (perimeter auth is
 * enforced by the gateway); they do not re-verify the JWT.
 */
public record UserContext(UUID userId, List<String> roles, String correlationId) {

    /** Request-attribute key under which the resolved context is stored. */
    public static final String ATTRIBUTE = "matchly.userContext";

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
