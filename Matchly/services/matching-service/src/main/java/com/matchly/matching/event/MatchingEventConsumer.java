package com.matchly.matching.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchly.matching.event.MatchingEventPayloads.ApplicationSubmitted;
import com.matchly.matching.service.MatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code application.events}. On {@code ApplicationSubmitted} it triggers
 * scoring of that candidate against the job (fetch → AI score → persist → emit
 * {@code MatchScored}). Values arrive as JSON strings (StringDeserializer); the
 * envelope is parsed leniently. Failures are logged, not rethrown, so a bad or
 * transient event never wedges the consumer — re-scoring is idempotent (upsert).
 */
@Component
public class MatchingEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(MatchingEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final MatchingService matchingService;

    public MatchingEventConsumer(ObjectMapper objectMapper, MatchingService matchingService) {
        this.objectMapper = objectMapper;
        this.matchingService = matchingService;
    }

    @KafkaListener(
            topics = "${matchly.kafka.topics.application-events:application.events}",
            groupId = "${spring.kafka.consumer.group-id:matching-service}")
    public void onApplicationEvent(String message) {
        try {
            IncomingEnvelope envelope = objectMapper.readValue(message, IncomingEnvelope.class);
            if (!"ApplicationSubmitted".equals(envelope.eventType())) {
                log.debug("Ignoring application event type {}", envelope.eventType());
                return;
            }
            ApplicationSubmitted payload =
                    objectMapper.treeToValue(envelope.payload(), ApplicationSubmitted.class);
            if (payload == null || payload.candidateId() == null || payload.jobId() == null) {
                log.warn("ApplicationSubmitted missing candidateId/jobId; skipping: {}", message);
                return;
            }
            matchingService.computeAndStore(
                    payload.candidateId(), payload.jobId(), envelope.correlationId());
        } catch (Exception ex) {
            // Fail-soft on the async path: log and move on (offset still commits).
            log.error("Failed to process application event: {}", ex.getMessage(), ex);
        }
    }
}
