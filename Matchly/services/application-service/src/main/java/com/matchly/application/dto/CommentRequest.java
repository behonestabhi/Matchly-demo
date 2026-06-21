package com.matchly.application.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for {@code POST /api/v1/applications/{id}/comments}. */
public record CommentRequest(
        @NotBlank(message = "body is required") String body,
        String visibility) {
}
