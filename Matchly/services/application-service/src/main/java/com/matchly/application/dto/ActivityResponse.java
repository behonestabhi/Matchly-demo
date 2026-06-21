package com.matchly.application.dto;

import com.matchly.application.domain.ActivityLog;
import java.time.Instant;
import java.util.UUID;

/** Response shape for an activity-log entry. */
public record ActivityResponse(
        UUID id,
        UUID applicationId,
        UUID actorId,
        String action,
        String payload,
        Instant occurredAt) {

    public static ActivityResponse from(ActivityLog a) {
        return new ActivityResponse(
                a.getId(),
                a.getApplicationId(),
                a.getActorId(),
                a.getAction(),
                a.getPayload(),
                a.getOccurredAt());
    }
}
