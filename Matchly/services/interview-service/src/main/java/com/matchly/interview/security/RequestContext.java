package com.matchly.interview.security;

import java.util.List;

/**
 * Holds the caller identity for the duration of a request, populated by
 * {@link HeaderAuthFilter} from gateway-forwarded headers.
 */
public final class RequestContext {

    /** Resolved caller identity. Fields may be null when called directly (no gateway). */
    public record Principal(String userId, List<String> roles, String correlationId) {
        public Principal {
            roles = roles == null ? List.of() : List.copyOf(roles);
        }
    }

    private static final ThreadLocal<Principal> CURRENT =
            ThreadLocal.withInitial(() -> new Principal(null, List.of(), null));

    private RequestContext() {
    }

    public static void set(Principal principal) {
        CURRENT.set(principal);
    }

    public static Principal get() {
        return CURRENT.get();
    }

    public static String correlationId() {
        return CURRENT.get().correlationId();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
