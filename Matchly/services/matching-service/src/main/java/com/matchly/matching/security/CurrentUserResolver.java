package com.matchly.matching.security;

import com.matchly.matching.exception.ForbiddenException;
import com.matchly.matching.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Reads the {@link CurrentUser} placed on the request by {@link HeaderAuthFilter}
 * and enforces simple authorization rules.
 */
@Component
public class CurrentUserResolver {

    public CurrentUser resolve(HttpServletRequest request) {
        Object attr = request.getAttribute(CurrentUser.ATTRIBUTE);
        if (attr instanceof CurrentUser cu) {
            return cu;
        }
        return new CurrentUser(null, List.of());
    }

    public UUID requireUserId(HttpServletRequest request) {
        CurrentUser user = resolve(request);
        if (user.userId() == null) {
            throw new UnauthorizedException("Missing or invalid X-User-Id");
        }
        return user.userId();
    }

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
