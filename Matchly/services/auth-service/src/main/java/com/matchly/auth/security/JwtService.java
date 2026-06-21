package com.matchly.auth.security;

import com.matchly.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies HS256 access tokens.
 *
 * <p>Claims: {@code sub} (userId), {@code email}, {@code roles} (list), plus a
 * unique {@code jti}. Uses the jjwt 0.12.x fluent API.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessTtlSeconds;
    private final String issuer;

    public JwtService(JwtProperties properties) {
        // HS256 requires a key of at least 256 bits (32 bytes).
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = properties.accessTtlSeconds();
        this.issuer = properties.issuer();
    }

    /** Generate a signed access token; returns the compact JWT and its jti. */
    public IssuedAccessToken generateAccessToken(UUID userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTtlSeconds);
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .id(jti)
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)               // alg inferred from key (HS256)
                .compact();

        return new IssuedAccessToken(token, jti, accessTtlSeconds, expiry);
    }

    /** Parse and verify a token's signature and expiry. Throws on invalid token. */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    /** Holder for an issued access token. */
    public record IssuedAccessToken(String token, String jti, long expiresIn, Instant expiresAt) {
    }
}
