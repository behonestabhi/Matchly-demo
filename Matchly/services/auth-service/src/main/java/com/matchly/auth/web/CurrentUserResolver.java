package com.matchly.auth.web;

import com.matchly.auth.exception.UnauthorizedException;
import com.matchly.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves the calling user's identity from either the gateway-forwarded
 * {@code X-User-Id} header (trusted perimeter) or, when called directly, a
 * verified {@code Authorization: Bearer} token.
 */
@Component
public class CurrentUserResolver {

    private final JwtService jwtService;

    public CurrentUserResolver(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /** @return the resolved user id, or throw 401 if neither source yields one. */
    public UUID requireUserId(HttpServletRequest request) {
        String headerId = request.getHeader("X-User-Id");
        if (StringUtils.hasText(headerId)) {
            try {
                return UUID.fromString(headerId.trim());
            } catch (IllegalArgumentException e) {
                throw new UnauthorizedException("Invalid X-User-Id header");
            }
        }
        Claims claims = parseBearer(request);
        try {
            return UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException("Token subject is not a valid user id");
        }
    }

    /** @return roles for the caller, from the {@code X-User-Roles} header or the bearer token. */
    @SuppressWarnings("unchecked")
    public List<String> resolveRoles(HttpServletRequest request) {
        String headerRoles = request.getHeader("X-User-Roles");
        if (StringUtils.hasText(headerRoles)) {
            return List.of(headerRoles.split(","));
        }
        try {
            Claims claims = parseBearer(request);
            Object roles = claims.get("roles");
            if (roles instanceof List<?> list) {
                return list.stream().map(String::valueOf).toList();
            }
        } catch (UnauthorizedException ignored) {
            // no bearer token present
        }
        return List.of();
    }

    private Claims parseBearer(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing authentication");
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        try {
            return jwtService.parse(token);
        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid or expired token");
        }
    }
}
