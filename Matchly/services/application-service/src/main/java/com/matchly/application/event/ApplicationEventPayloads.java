package com.matchly.application.event;

import java.util.UUID;

/**
 * Payload shapes for events produced to {@code application.events}
 * (see {@code libs/schemas/events.md}).
 */
public final class ApplicationEventPayloads {

    private ApplicationEventPayloads() {
    }

    /** {@code ApplicationSubmitted} payload: {@code { applicationId, candidateId, jobId }}. */
    public record ApplicationSubmitted(UUID applicationId, UUID candidateId, UUID jobId) {
    }

    /** {@code StageChanged} payload: {@code { applicationId, fromStage, toStage, actorId }}. */
    public record StageChanged(UUID applicationId, String fromStage, String toStage, UUID actorId) {
    }
}
