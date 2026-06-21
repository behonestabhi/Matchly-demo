package com.matchly.analytics.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Common Kafka event envelope shared across Matchly
 * (see {@code libs/schemas/event-envelope.schema.json}):
 * {@code {eventId, eventType, occurredAt, correlationId, version, payload}}.
 *
 * <p>The payload is kept as a Jackson {@link JsonNode} because each event type
 * carries a different body; listeners read the fields they need.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope(
        String eventId,
        String eventType,
        String occurredAt,
        String correlationId,
        Integer version,
        JsonNode payload) {
}
