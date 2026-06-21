package com.matchly.application.security;

import com.matchly.application.exception.ForbiddenException;
import com.matchly.application.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Reads the {@link CurrentUser} placed on the request by {@link HeaderAuthFilter}
 * and enforces simple authorization rules (presence of an identity, role checks).
 */
@Component
public class CurrentUserResolver {

    /** @return the current user (possibly anonymous) attached by the filter. */
    public CurrentUser resolve(HttpServletRequest request) {
        Object attr = request.getAttribute(CurrentUser.ATTRIBUTE);
        if (attr instanceof CurrentUser cu) {
            return cu;
        }
        return new CurrentUser(null, List.of());
    }

    /** @return the authenticated user id, or 401 if absent. */
    public UUID requireUserId(HttpServletRequest request) {
        CurrentUser user = resolve(request);
        if (user.userId() == null) {
            throw new UnauthorizedException("Missing or invalid X-User-Id");
        }
        return user.userId();
    }

    /** Require the caller to hold at least one of the given roles, else 403. */
    public void requireAnyRole(HttpServletRequest request, String... roles) {
        CurrentUser user = resolve(request);
        if (user.userId() == null) {
            throw new UnauthorizedException("Missing authentication");
        }
        if (!user.hasAnyRole(roles)) {
            throw new ForbiddenException("Requires one of roles: " + String.join(",", roles));
        }
    }
}
