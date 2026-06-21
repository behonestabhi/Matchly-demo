package com.matchly.job.security;

import com.matchly.job.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Reads the {@link UserContext} that {@link HeaderAuthFilter} placed on the
 * request. {@code requireUserId} fails with 401 when the gateway did not forward
 * an identity (e.g. an unauthenticated call that bypassed the gateway).
 */
@Component
public class CurrentUserResolver {

    /** @return the calling user's id, or 401 if the gateway did not forward one. */
    public UUID requireUserId(HttpServletRequest request) {
        UserContext ctx = current(request);
        if (ctx == null) {
            throw new UnauthorizedException("Missing X-User-Id header (gateway identity not forwarded)");
        }
        return ctx.userId();
    }

    /** @return the caller's roles, or an empty list when no identity is present. */
    public List<String> resolveRoles(HttpServletRequest request) {
        UserContext ctx = current(request);
        return ctx == null ? List.of() : ctx.roles();
    }

    /** @return the correlation id forwarded by the gateway, or {@code null}. */
    public String correlationId(HttpServletRequest request) {
        UserContext ctx = current(request);
        return ctx == null ? null : ctx.correlationId();
    }

    /** @return the resolved context, or {@code null} if none was set. */
    public UserContext current(HttpServletRequest request) {
        Object attr = request.getAttribute(UserContext.ATTRIBUTE);
        return attr instanceof UserContext ctx ? ctx : null;
    }
}
