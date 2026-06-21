package com.matchly.auth.dto;

import com.matchly.auth.domain.Role;
import com.matchly.auth.domain.User;
import java.util.List;
import java.util.UUID;

/** Public-facing view of a user. Never exposes the password hash. */
public record UserSummary(
        UUID id,
        String email,
        String fullName,
        String status,
        boolean emailVerified,
        List<String> roles
) {
    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getRoles().stream().map(Role::getName).sorted().toList()
        );
    }
}
