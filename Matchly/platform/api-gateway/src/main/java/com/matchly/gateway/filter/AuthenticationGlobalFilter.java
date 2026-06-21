package com.matchly.gateway.filter;

import com.matchly.gateway.config.GatewayProperties;
import com.matchly.gateway.security.JwtValidator;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * The gateway's edge filter. For every request it:
 * <ol>
 *   <li>ensures an {@code X-Correlation-Id} (generating one if absent) and
 *       echoes it back on the response;</li>
 *   <li>lets public paths through untouched;</li>
 *   <li>on protected paths, validates the bearer JWT (HS256 signature + expiry)
 *       and, on success, injects trusted {@code X-User-Id} / {@code X-User-Roles}
 *       headers downstream — stripping any client-supplied copies first;</li>
 *   <li>returns {@code 401} on a missing/invalid token.</li>
 * </ol>
 */
@Component
public class AuthenticationGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationGlobalFilter.class);

    public static final String CORRELATION_ID = "X-Correlation-Id";
    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLES = "X-User-Roles";

    private final GatewayProperties properties;
    private final JwtValidator jwtValidator;

    public AuthenticationGlobalFilter(GatewayProperties properties, JwtValidator jwtValidator) {
        this.properties = properties;
        this.jwtValidator = jwtValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Correlation id (propagate or generate).
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID);
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        final String corrId = correlationId;
        exchange.getResponse().getHeaders().set(CORRELATION_ID, corrId);

        // 2. Public paths: only add the correlation id, never trust inbound identity headers.
        if (properties.isPublic(path)) {
            ServerHttpRequest mutated = request.mutate()
                    .headers(h -> {
                        h.set(CORRELATION_ID, corrId);
                        h.remove(USER_ID);
                        h.remove(USER_ROLES);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        // 3. Protected paths: require a valid bearer token.
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, corrId, "Missing or malformed Authorization header");
        }
        String token = authHeader.substring("Bearer ".length()).trim();

        try {
            Claims claims = jwtValidator.validate(token);
            String userId = claims.getSubject();
            String roles = jwtValidator.rolesHeader(claims);

            ServerHttpRequest mutated = request.mutate()
                    .headers(h -> {
                        h.set(CORRELATION_ID, corrId);
                        h.set(USER_ID, userId);
                        h.set(USER_ROLES, roles);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed [{}]: {}", corrId, e.getMessage());
            return unauthorized(exchange, corrId, "Invalid or expired token");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String correlationId, String detail) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        response.getHeaders().set(CORRELATION_ID, correlationId);

        String body = String.format(
                "{\"type\":\"about:blank\",\"title\":\"Unauthorized\",\"status\":401,"
                        + "\"detail\":\"%s\",\"correlationId\":\"%s\"}",
                detail, correlationId);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // Run early, before routing/filters that need the identity headers.
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
