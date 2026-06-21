package com.matchly.job.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lightweight perimeter-trust filter.
 *
 * <p>The API gateway validates the JWT and forwards trusted identity headers
 * ({@code X-User-Id}, {@code X-User-Roles}, {@code X-Correlation-Id}). This
 * filter reads them into a {@link UserContext} request attribute. Downstream
 * code trusts this context; this service does <em>not</em> re-verify the JWT.
 */
@Component
@Order(0)
public class HeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rawUserId = request.getHeader("X-User-Id");
        if (StringUtils.hasText(rawUserId)) {
            UUID userId = tryParseUuid(rawUserId.trim());
            if (userId != null) {
                List<String> roles = parseRoles(request.getHeader("X-User-Roles"));
                String correlationId = request.getHeader("X-Correlation-Id");
                request.setAttribute(UserContext.ATTRIBUTE,
                        new UserContext(userId, roles, correlationId));
            }
        }
        filterChain.doFilter(request, response);
    }

    private static UUID tryParseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static List<String> parseRoles(String header) {
        if (!StringUtils.hasText(header)) {
            return List.of();
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
