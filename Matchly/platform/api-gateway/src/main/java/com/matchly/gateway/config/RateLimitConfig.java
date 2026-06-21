package com.matchly.gateway.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Optional Redis-backed rate limiting.
 *
 * <p>Disabled by default ({@code gateway.rate-limit.enabled=false}) so the
 * gateway boots and routes even when Redis is absent. Enable it (and ensure
 * Redis is reachable) by setting {@code gateway.rate-limit.enabled=true} or
 * {@code RATE_LIMIT_ENABLED=true}. To enforce the limit on a route, attach the
 * {@code RequestRateLimiter} filter referencing these beans in route config.
 */
@Configuration
@ConditionalOnProperty(prefix = "gateway.rate-limit", name = "enabled", havingValue = "true")
public class RateLimitConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter(GatewayProperties properties) {
        GatewayProperties.RateLimit rl = properties.getRateLimit();
        return new RedisRateLimiter(rl.getReplenishRate(), rl.getBurstCapacity(), rl.getRequestedTokens());
    }

    /**
     * Rate-limit key: the authenticated user (injected upstream) when present,
     * otherwise the client IP. Falls back to a constant for anonymous requests
     * with no resolvable remote address.
     */
    @Bean
    public KeyResolver gatewayKeyResolver() {
        return exchange -> Mono.just(resolveKey(exchange));
    }

    private String resolveKey(ServerWebExchange exchange) {
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (StringUtils.hasText(userId)) {
            return "user:" + userId;
        }
        if (exchange.getRequest().getRemoteAddress() != null) {
            return "ip:" + exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "anonymous";
    }
}
