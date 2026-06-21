package com.matchly.application.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for {@code PATCH /api/v1/applications/{id}/stage}. */
public record StageChangeRequest(
        @NotBlank(message = "toStage is required") String toStage,
        String reason) {
}
