package com.matchly.application.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lightweight perimeter-trust filter.
 *
 * <p>This service sits behind the API gateway, which validates the JWT and
 * forwards trusted identity headers downstream. The filter reads
 * {@code X-User-Id} and {@code X-User-Roles} (comma-separated) and exposes a
 * {@link CurrentUser} on the request attribute. It never rejects requests —
 * authorization decisions are made by controllers / services via
 * {@link CurrentUserResolver}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HeaderAuthFilter extends OncePerRequestFilter {

    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        UUID userId = parseUserId(request.getHeader(USER_ID_HEADER));
        List<String> roles = parseRoles(request.getHeader(USER_ROLES_HEADER));
        request.setAttribute(CurrentUser.ATTRIBUTE, new CurrentUser(userId, roles));
        filterChain.doFilter(request, response);
    }

    private UUID parseUserId(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<String> parseRoles(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
