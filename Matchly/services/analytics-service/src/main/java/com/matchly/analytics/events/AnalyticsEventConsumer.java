package com.matchly.analytics.events;

import com.fasterxml.jackson.databind.JsonNode;
import com.matchly.analytics.service.IdempotencyService;
import com.matchly.analytics.service.ProjectionService;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka consumers feeding the analytics read models. Each topic has its own
 * listener; every message is the common {@link EventEnvelope}. Processing is
 * guarded by {@link IdempotencyService} (dedup by {@code eventId}) so replays
 * are no-ops.
 *
 * <p>Topics: {@code application.events} (ApplicationSubmitted, StageChanged),
 * {@code matching.events} (MatchScored), {@code job.events} (JobPosted),
 * {@code interview.events} (QuestionsGenerated).
 */
@Component
public class AnalyticsEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsEventConsumer.class);

    private final IdempotencyService idempotencyService;
    private final ProjectionService projectionService;

    public AnalyticsEventConsumer(IdempotencyService idempotencyService,
                                  ProjectionService projectionService) {
        this.idempotencyService = idempotencyService;
        this.projectionService = projectionService;
    }

    @KafkaListener(
            topics = "${matchly.topics.application-events:application.events}",
            groupId = "${spring.kafka.consumer.group-id:analytics-service}")
    @Transactional
    public void onApplicationEvent(EventEnvelope envelope) {
        if (!shouldProcess(envelope)) {
            return;
        }
        JsonNode p = envelope.payload();
        Instant occurredAt = occurredAt(envelope);
        switch (safeType(envelope)) {
            case "ApplicationSubmitted" -> projectionService.onApplicationSubmitted(
                    PayloadReader.uuid(p, "applicationId"),
                    PayloadReader.uuid(p, "candidateId"),
                    PayloadReader.uuid(p, "jobId"),
                    occurredAt);
            case "StageChanged" -> projectionService.onStageChanged(
                    PayloadReader.uuid(p, "applicationId"),
                    PayloadReader.uuid(p, "jobId"),
                    PayloadReader.text(p, "fromStage"),
                    PayloadReader.text(p, "toStage"),
                    occurredAt);
            default -> log.debug("Ignoring application event type {}", safeType(envelope));
        }
    }

    @KafkaListener(
            topics = "${matchly.topics.matching-events:matching.events}",
            groupId = "${spring.kafka.consumer.group-id:analytics-service}")
    @Transactional
    public void onMatchingEvent(EventEnvelope envelope) {
        if (!shouldProcess(envelope)) {
            return;
        }
        JsonNode p = envelope.payload();
        if ("MatchScored".equals(safeType(envelope))) {
            projectionService.onMatchScored(
                    PayloadReader.uuid(p, "candidateId"),
                    PayloadReader.uuid(p, "jobId"),
                    PayloadReader.doubleVal(p, "finalScore"));
        } else {
            log.debug("Ignoring matching event type {}", safeType(envelope));
        }
    }

    @KafkaListener(
            topics = "${matchly.topics.job-events:job.events}",
            groupId = "${spring.kafka.consumer.group-id:analytics-service}")
    public void onJobEvent(EventEnvelope envelope) {
        if (!shouldProcess(envelope)) {
            return;
        }
        JsonNode p = envelope.payload();
        if ("JobPosted".equals(safeType(envelope))) {
            projectionService.onJobPosted(
                    PayloadReader.uuid(p, "jobId"),
                    PayloadReader.skills(p),
                    occurredAt(envelope));
        } else {
            log.debug("Ignoring job event type {}", safeType(envelope));
        }
    }

    @KafkaListener(
            topics = "${matchly.topics.interview-events:interview.events}",
            groupId = "${spring.kafka.consumer.group-id:analytics-service}")
    public void onInterviewEvent(EventEnvelope envelope) {
        if (!shouldProcess(envelope)) {
            return;
        }
        JsonNode p = envelope.payload();
        if ("QuestionsGenerated".equals(safeType(envelope))) {
            projectionService.onQuestionsGenerated(
                    PayloadReader.uuid(p, "candidateId"),
                    PayloadReader.uuid(p, "jobId"),
                    PayloadReader.uuid(p, "questionSetId"));
        } else {
            log.debug("Ignoring interview event type {}", safeType(envelope));
        }
    }

    /** Idempotency claim + null-guard. Returns true when the event should be processed. */
    private boolean shouldProcess(EventEnvelope envelope) {
        if (envelope == null) {
            return false;
        }
        boolean firstTime = idempotencyService.claim(envelope.eventId(), safeType(envelope));
        if (!firstTime) {
            log.debug("Skipping already-processed event {} ({})", envelope.eventId(), safeType(envelope));
        }
        return firstTime;
    }

    private String safeType(EventEnvelope envelope) {
        return envelope.eventType() == null ? "" : envelope.eventType();
    }

    private Instant occurredAt(EventEnvelope envelope) {
        Instant parsed = PayloadReader.instant(envelope.occurredAt());
        return parsed != null ? parsed : Instant.now();
    }
}
