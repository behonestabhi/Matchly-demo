package com.matchly.interview.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/**
 * Common Kafka event envelope shared across Matchly
 * (see {@code libs/schemas/event-envelope.schema.json}):
 * {@code {eventId, eventType, occurredAt, correlationId, version, payload}}.
 *
 * @param <T> payload type
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventEnvelope<T>(
        String eventId,
        String eventType,
        String occurredAt,
        String correlationId,
        int version,
        T payload) {

    /** Build an envelope with a fresh eventId and the current UTC timestamp (ISO-8601). */
    public static <T> EventEnvelope<T> create(String eventType, String correlationId, T payload) {
        return new EventEnvelope<>(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now().toString(),
                correlationId,
                1,
                payload);
    }
}
