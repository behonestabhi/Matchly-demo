package com.matchly.application.dto;

import com.matchly.application.domain.Application;
import java.time.Instant;
import java.util.UUID;

/** Response shape for an application (pipeline board / tracking views). */
public record ApplicationResponse(
        UUID id,
        UUID candidateId,
        UUID jobId,
        String currentStage,
        String source,
        Instant appliedAt,
        Instant lastUpdated) {

    public static ApplicationResponse from(Application a) {
        return new ApplicationResponse(
                a.getId(),
                a.getCandidateId(),
                a.getJobId(),
                a.getCurrentStage().name(),
                a.getSource(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }
}
