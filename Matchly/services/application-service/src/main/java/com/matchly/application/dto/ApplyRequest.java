package com.matchly.application.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Request body for {@code POST /api/v1/applications}. */
public record ApplyRequest(
        @NotNull(message = "jobId is required") UUID jobId,
        String source) {
}
