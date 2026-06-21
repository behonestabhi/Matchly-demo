package com.matchly.analytics.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lightweight perimeter-trust filter. The API gateway validates the JWT and
 * forwards trusted identity headers; this service simply reads them. No JWT
 * verification happens here (the gateway is the enforced perimeter — see
 * CONVENTIONS.md "Auth model"). Analytics endpoints are read-only, so the
 * headers are made available but not enforced beyond their presence.
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
        // Headers (X-User-Id / X-User-Roles / X-Correlation-Id) are trusted as
        // forwarded by the gateway. Read-only analytics imposes no extra check;
        // they remain available on the request for downstream use/logging.
        filterChain.doFilter(request, response);
    }
}
