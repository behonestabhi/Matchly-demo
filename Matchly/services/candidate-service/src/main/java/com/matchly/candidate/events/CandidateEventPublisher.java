package com.matchly.candidate.events;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Produces candidate-domain events onto Kafka, wrapped in the common
 * {@link EventEnvelope}. Topics and partition keys follow DATA_MODELS §9:
 * {@code candidate.events} and {@code resume.parsed} are both keyed by
 * {@code candidateId} so a candidate's events keep per-entity ordering.
 */
@Component
public class CandidateEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CandidateEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String candidateEventsTopic;
    private final String resumeParsedTopic;

    public CandidateEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${kafka.topics.candidate-events:candidate.events}") String candidateEventsTopic,
            @Value("${kafka.topics.resume-parsed:resume.parsed}") String resumeParsedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.candidateEventsTopic = candidateEventsTopic;
        this.resumeParsedTopic = resumeParsedTopic;
    }

    /** {@code ResumeUploaded} → {@code candidate.events} (key = candidateId). */
    public void publishResumeUploaded(UUID candidateId, UUID resumeId, String s3Key,
                                      String fileName, String mimeType, String correlationId) {
        ResumeUploaded payload = new ResumeUploaded(
                candidateId.toString(), resumeId.toString(), s3Key, fileName, mimeType);
        send(candidateEventsTopic, candidateId.toString(),
                EventEnvelope.of("ResumeUploaded", correlationId, payload));
    }

    /** {@code ResumeParsed} → {@code resume.parsed} (key = candidateId). */
    public void publishResumeParsed(UUID candidateId, UUID resumeId, List<String> skills,
                                    Double totalExpYrs, UUID embeddingRef, String correlationId) {
        ResumeParsed payload = new ResumeParsed(
                candidateId.toString(), resumeId.toString(), skills, totalExpYrs,
                embeddingRef == null ? null : embeddingRef.toString());
        send(resumeParsedTopic, candidateId.toString(),
                EventEnvelope.of("ResumeParsed", correlationId, payload));
    }

    private void send(String topic, String key, EventEnvelope envelope) {
        try {
            kafkaTemplate.send(topic, key, envelope)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish {} to topic {} (key={})",
                                    envelope.eventType(), topic, key, ex);
                        } else {
                            log.debug("Published {} to topic {} (key={})",
                                    envelope.eventType(), topic, key);
                        }
                    });
        } catch (Exception ex) {
            // Producing must never break the calling flow.
            log.error("Error sending {} to topic {} (key={})",
                    envelope.eventType(), topic, key, ex);
        }
    }

    /** Payload for {@code ResumeUploaded} (events.md). */
    public record ResumeUploaded(String candidateId, String resumeId, String s3Key,
                                 String fileName, String mimeType) {
    }

    /** Payload for {@code ResumeParsed} (events.md). */
    public record ResumeParsed(String candidateId, String resumeId, List<String> skills,
                               Double totalExpYrs, String embeddingRef) {
    }
}
