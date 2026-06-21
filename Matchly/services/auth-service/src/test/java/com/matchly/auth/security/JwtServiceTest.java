package com.matchly.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.matchly.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-definitely-long-enough-32";

    private JwtService newService() {
        return new JwtService(new JwtProperties(SECRET, 900, 1209600, "matchly-auth"));
    }

    @Test
    void issuesAndParsesAccessToken() {
        JwtService service = newService();
        UUID userId = UUID.randomUUID();

        JwtService.IssuedAccessToken issued =
                service.generateAccessToken(userId, "user@example.com", List.of("CANDIDATE"));

        Claims claims = service.parse(issued.token());
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email")).isEqualTo("user@example.com");
        assertThat(claims.get("roles", List.class)).containsExactly("CANDIDATE");
        assertThat(claims.getId()).isEqualTo(issued.jti());
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        JwtService service = newService();
        JwtService other = new JwtService(
                new JwtProperties("a-totally-different-secret-key-of-length-32!", 900, 1209600, "matchly-auth"));

        String token = other.generateAccessToken(
                UUID.randomUUID(), "x@y.com", List.of("ADMIN")).token();

        assertThatThrownBy(() -> service.parse(token)).isInstanceOf(JwtException.class);
    }
}
