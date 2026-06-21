package com.matchly.application.security;

import java.util.List;
import java.util.UUID;

/**
 * The caller's identity as forwarded by the API gateway via trusted headers
 * ({@code X-User-Id} / {@code X-User-Roles}). Stored on the request by
 * {@link HeaderAuthFilter} and read by controllers.
 *
 * @param userId the authenticated user id, or {@code null} for anonymous calls
 * @param roles  the caller's roles (never {@code null})
 */
public record CurrentUser(UUID userId, List<String> roles) {

    public static final String ATTRIBUTE = "matchly.currentUser";

    public boolean hasAnyRole(String... candidates) {
        for (String role : candidates) {
            if (roles.contains(role)) {
                return true;
            }
        }
        return false;
    }
}
