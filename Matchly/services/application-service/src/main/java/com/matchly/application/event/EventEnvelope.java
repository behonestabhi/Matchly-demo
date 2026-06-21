package com.matchly.application.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/**
 * Common Kafka event envelope shared by every Matchly domain event
 * (see {@code libs/schemas/event-envelope.schema.json}).
 *
 * <pre>{ eventId, eventType, occurredAt, correlationId, version, payload }</pre>
 *
 * @param eventId       dedup/idempotency key
 * @param eventType     e.g. {@code ApplicationSubmitted}, {@code StageChanged}
 * @param occurredAt    ISO-8601 instant the event happened
 * @param correlationId propagated from the gateway for tracing (nullable)
 * @param version       schema version (>= 1)
 * @param payload       event-type-specific body
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String correlationId,
        int version,
        Object payload) {

    public static EventEnvelope of(String eventType, String correlationId, Object payload) {
        return new EventEnvelope(UUID.randomUUID(), eventType, Instant.now(), correlationId, 1, payload);
    }
}
