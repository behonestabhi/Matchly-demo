package com.matchly.job.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Common Kafka event envelope shared by all Matchly services
 * (see {@code libs/schemas/event-envelope.schema.json} and CONVENTIONS.md):
 * {@code { eventId, eventType, occurredAt, correlationId, version, payload }}.
 */
public record EventEnvelope(
        String eventId,
        String eventType,
        Instant occurredAt,
        String correlationId,
        int version,
        Object payload
) {
    /** Build an envelope with a fresh eventId and {@code occurredAt = now}. */
    public static EventEnvelope of(String eventType, String correlationId, Object payload) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now(),
                correlationId,
                1,
                payload);
    }
}
