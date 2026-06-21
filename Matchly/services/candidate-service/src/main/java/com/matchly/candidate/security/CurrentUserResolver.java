package com.matchly.candidate.security;

import com.matchly.candidate.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Reads the {@link UserContext} that {@link HeaderAuthFilter} placed on the
 * request. Provides a convenient {@code requireUserId} that fails with 401 when
 * the gateway did not forward an identity.
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

    /** @return the resolved context, or {@code null} if none was set. */
    public UserContext current(HttpServletRequest request) {
        Object attr = request.getAttribute(UserContext.ATTRIBUTE);
        return attr instanceof UserContext ctx ? ctx : null;
    }
}
