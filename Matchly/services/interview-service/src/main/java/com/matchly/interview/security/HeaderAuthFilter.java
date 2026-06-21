package com.matchly.interview.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lightweight perimeter-trust filter. The API gateway validates the JWT and
 * forwards trusted identity headers; this service simply reads them into a
 * per-request {@link RequestContext}. No JWT verification happens here (the
 * gateway is the enforced perimeter — see CONVENTIONS.md "Auth model").
 */
@Component
@Order(1)
public class HeaderAuthFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";
    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String userId = request.getHeader(USER_ID_HEADER);
            String rolesHeader = request.getHeader(USER_ROLES_HEADER);
            String correlationId = request.getHeader(CORRELATION_HEADER);

            List<String> roles = StringUtils.hasText(rolesHeader)
                    ? List.of(rolesHeader.split(","))
                    : List.of();

            RequestContext.set(new RequestContext.Principal(
                    StringUtils.hasText(userId) ? userId.trim() : null,
                    roles,
                    StringUtils.hasText(correlationId) ? correlationId.trim() : null));

            filterChain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
    }
}
