package com.matchly.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Body for {@code POST /api/v1/auth/users/{id}/roles}.
 *
 * @param op    ASSIGN or REVOKE
 * @param roles role names to apply (e.g. ["RECRUITER","ADMIN"])
 */
public record RoleAssignmentRequest(
        @NotNull Operation op,
        @NotEmpty List<String> roles
) {
    public enum Operation {
        ASSIGN,
        REVOKE
    }
}
