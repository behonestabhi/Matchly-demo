package com.matchly.matching.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Lenient view of an inbound event envelope. {@code payload} is left as a raw
 * {@link JsonNode} so the consumer can branch on {@code eventType} before
 * binding the payload to a concrete type. Unknown envelope fields are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IncomingEnvelope(
        String eventId,
        String eventType,
        String correlationId,
        JsonNode payload) {
}
