package com.matchly.auth.service;

import com.matchly.auth.config.JwtProperties;
import com.matchly.auth.domain.RefreshToken;
import com.matchly.auth.domain.Role;
import com.matchly.auth.domain.User;
import com.matchly.auth.dto.LoginRequest;
import com.matchly.auth.dto.RefreshRequest;
import com.matchly.auth.dto.RegisterRequest;
import com.matchly.auth.dto.RoleAssignmentRequest;
import com.matchly.auth.dto.TokenResponse;
import com.matchly.auth.dto.UserSummary;
import com.matchly.auth.exception.ConflictException;
import com.matchly.auth.exception.NotFoundException;
import com.matchly.auth.exception.UnauthorizedException;
import com.matchly.auth.repository.RefreshTokenRepository;
import com.matchly.auth.repository.RoleRepository;
import com.matchly.auth.repository.UserRepository;
import com.matchly.auth.security.JwtService;
import com.matchly.auth.security.TokenHasher;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Core authentication logic: registration, login, token rotation, role management. */
@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "CANDIDATE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHasher tokenHasher;
    private final long refreshTtlSeconds;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenHasher tokenHasher,
                       JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenHasher = tokenHasher;
        this.refreshTtlSeconds = jwtProperties.refreshTtlSeconds();
    }

    @Transactional
    public UserSummary register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("An account with that email already exists");
        }
        Role candidateRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Default role " + DEFAULT_ROLE + " not seeded"));

        User user = new User(
                UUID.randomUUID(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName());
        Set<Role> roles = new HashSet<>();
        roles.add(candidateRole);
        user.setRoles(roles);

        return UserSummary.from(userRepository.save(user));
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UnauthorizedException("Account is not active");
        }
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String presentedHash = tokenHasher.hash(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(presentedHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!stored.isActive()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }
        // Rotate: revoke the presented token, then issue a fresh pair.
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        String presentedHash = tokenHasher.hash(refreshToken);
        refreshTokenRepository.findByTokenHash(presentedHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
        // Idempotent: unknown/already-revoked tokens succeed silently.
    }

    @Transactional(readOnly = true)
    public UserSummary me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return UserSummary.from(user);
    }

    @Transactional
    public UserSummary updateRoles(UUID userId, RoleAssignmentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        Set<Role> current = new HashSet<>(user.getRoles());
        for (String roleName : request.roles()) {
            Role role = roleRepository.findByName(roleName.toUpperCase())
                    .orElseThrow(() -> new NotFoundException("Unknown role: " + roleName));
            if (request.op() == RoleAssignmentRequest.Operation.ASSIGN) {
                current.add(role);
            } else {
                current.remove(role);
            }
        }
        user.setRoles(current);
        return UserSummary.from(userRepository.save(user));
    }

    /** Issue a fresh access + refresh token pair for the user. */
    private TokenResponse issueTokens(User user) {
        List<String> roleNames = user.getRoles().stream().map(Role::getName).sorted().toList();
        JwtService.IssuedAccessToken access =
                jwtService.generateAccessToken(user.getId(), user.getEmail(), roleNames);

        String opaqueRefresh = tokenHasher.generateOpaqueToken();
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(),
                user.getId(),
                tokenHasher.hash(opaqueRefresh),
                Instant.now().plusSeconds(refreshTtlSeconds),
                UUID.fromString(access.jti()));
        refreshTokenRepository.save(refreshToken);

        return TokenResponse.bearer(access.token(), opaqueRefresh, access.expiresIn());
    }
}
