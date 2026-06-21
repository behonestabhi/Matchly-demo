package com.matchly.gateway.security;

import com.matchly.gateway.config.GatewayProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Validates HS256 access tokens at the edge using the shared {@code JWT_SECRET}.
 * Verifies the signature, issuer and expiry (expiry is enforced by the parser).
 */
@Component
public class JwtValidator {

    private final SecretKey key;
    private final String issuer;

    public JwtValidator(GatewayProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.issuer = properties.getJwt().getIssuer();
    }

    /**
     * Parse and verify a compact JWT.
     *
     * @throws io.jsonwebtoken.JwtException if the token is invalid, tampered or expired
     */
    public Claims validate(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Extract the {@code roles} claim as a comma-joined header value. */
    public String rolesHeader(Claims claims) {
        Object roles = claims.get("roles");
        if (roles instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return "";
    }
}
