package com.matchly.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.matchly.gateway.config.GatewayProperties;
import com.matchly.gateway.security.JwtValidator;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class AuthenticationGlobalFilterTest {

    private static final String SECRET = "test-secret-that-is-definitely-long-enough-32";

    private AuthenticationGlobalFilter filter;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        GatewayProperties props = new GatewayProperties();
        props.getJwt().setSecret(SECRET);
        props.getJwt().setIssuer("matchly-auth");
        filter = new AuthenticationGlobalFilter(props, new JwtValidator(props));
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void publicPathPassesWithoutToken() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        RecordingChain chain = new RecordingChain();
        filter.filter(exchange, chain).block();

        assertThat(chain.invoked).isTrue();
        assertThat(exchange.getResponse().getHeaders().getFirst("X-Correlation-Id")).isNotBlank();
    }

    @Test
    void protectedPathWithoutTokenIs401() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/jobs").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        RecordingChain chain = new RecordingChain();
        filter.filter(exchange, chain).block();

        assertThat(chain.invoked).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedPathWithValidTokenInjectsIdentityHeaders() {
        String userId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .issuer("matchly-auth")
                .subject(userId)
                .claim("roles", List.of("RECRUITER"))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/jobs")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        RecordingChain chain = new RecordingChain();
        filter.filter(exchange, chain).block();

        assertThat(chain.invoked).isTrue();
        ServerHttpRequest downstream = chain.captured.getRequest();
        assertThat(downstream.getHeaders().getFirst("X-User-Id")).isEqualTo(userId);
        assertThat(downstream.getHeaders().getFirst("X-User-Roles")).isEqualTo("RECRUITER");
    }

    /** Captures the (possibly mutated) exchange handed to the chain. */
    private static final class RecordingChain implements GatewayFilterChain {
        boolean invoked = false;
        ServerWebExchange captured;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            this.invoked = true;
            this.captured = exchange;
            return Mono.empty();
        }
    }
}
