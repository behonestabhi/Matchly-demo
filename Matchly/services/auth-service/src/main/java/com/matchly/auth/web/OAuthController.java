package com.matchly.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth2 endpoints. STUBBED in this build: the persistence model
 * ({@code oauth_accounts}) exists, but the provider handshake (google|linkedin)
 * is not wired up. Both endpoints return HTTP 501 Not Implemented.
 */
@RestController
@RequestMapping("/api/v1/auth/oauth")
@Tag(name = "Auth — OAuth (stub)", description = "OAuth2 sign-in — not implemented in this build")
public class OAuthController {

    private static final Map<String, Boolean> SUPPORTED = Map.of(
            "google", true,
            "linkedin", true);

    @Operation(summary = "Begin OAuth2 (STUB — returns 501)")
    @GetMapping("/{provider}")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public ProblemDetail begin(@PathVariable("provider") String provider, HttpServletRequest request) {
        return notImplemented(provider, request);
    }

    @Operation(summary = "Complete OAuth2 callback (STUB — returns 501)")
    @GetMapping("/{provider}/callback")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public ProblemDetail callback(@PathVariable("provider") String provider, HttpServletRequest request) {
        return notImplemented(provider, request);
    }

    private ProblemDetail notImplemented(String provider, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_IMPLEMENTED,
                "OAuth2 sign-in for '" + provider + "' is not implemented in this build. "
                        + "Use POST /api/v1/auth/register and /api/v1/auth/login instead.");
        problem.setTitle("Not implemented");
        problem.setProperty("provider", provider);
        problem.setProperty("recognizedProvider", SUPPORTED.getOrDefault(provider.toLowerCase(), false));
        String correlationId = request.getHeader("X-Correlation-Id");
        if (correlationId != null && !correlationId.isBlank()) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }
}
