package com.matchly.auth.web;

import com.matchly.auth.dto.LoginRequest;
import com.matchly.auth.dto.LogoutRequest;
import com.matchly.auth.dto.RefreshRequest;
import com.matchly.auth.dto.RegisterRequest;
import com.matchly.auth.dto.RoleAssignmentRequest;
import com.matchly.auth.dto.TokenResponse;
import com.matchly.auth.dto.UserSummary;
import com.matchly.auth.exception.ForbiddenException;
import com.matchly.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Auth Service HTTP API. All paths under {@code /api/v1/auth}. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Registration, login, token rotation and role management")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserResolver currentUserResolver;

    public AuthController(AuthService authService, CurrentUserResolver currentUserResolver) {
        this.authService = authService;
        this.currentUserResolver = currentUserResolver;
    }

    @Operation(summary = "Create an account (default role CANDIDATE)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummary register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Log in; returns access + refresh tokens")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Rotate the refresh token and issue a new access token")
    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @Operation(summary = "Revoke a refresh token (logout)")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
    }

    @Operation(summary = "Current user + roles (reads X-User-Id header or bearer token)")
    @GetMapping("/me")
    public UserSummary me(HttpServletRequest request) {
        UUID userId = currentUserResolver.requireUserId(request);
        return authService.me(userId);
    }

    @Operation(summary = "Assign or revoke roles on a user (ADMIN only)")
    @PostMapping("/users/{id}/roles")
    public UserSummary updateRoles(@PathVariable("id") UUID id,
                                   @Valid @RequestBody RoleAssignmentRequest request,
                                   HttpServletRequest httpRequest) {
        List<String> callerRoles = currentUserResolver.resolveRoles(httpRequest);
        if (!callerRoles.contains("ADMIN")) {
            throw new ForbiddenException("Only ADMIN may modify roles");
        }
        return authService.updateRoles(id, request);
    }

    @Operation(summary = "Liveness shortcut (also available at /actuator/health)")
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("auth-service ok");
    }
}
