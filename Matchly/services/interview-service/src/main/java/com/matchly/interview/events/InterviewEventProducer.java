package com.matchly.interview.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@code QuestionsGenerated} events to {@code interview.events},
 * keyed by {@code candidateId} (per DATA_MODELS §9 partition key) to preserve
 * per-candidate ordering.
 */
@Component
public class InterviewEventProducer {

    private static final Logger log = LoggerFactory.getLogger(InterviewEventProducer.class);
    private static final String EVENT_TYPE = "QuestionsGenerated";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public InterviewEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                                  @Value("${matchly.topics.interview-events:interview.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /** Publish a QuestionsGenerated event keyed by candidateId. */
    public void publishQuestionsGenerated(QuestionsGeneratedPayload payload, String correlationId) {
        EventEnvelope<QuestionsGeneratedPayload> envelope =
                EventEnvelope.create(EVENT_TYPE, correlationId, payload);
        String key = payload.candidateId() == null ? null : payload.candidateId().toString();
        kafkaTemplate.send(topic, key, envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} for questionSet {}: {}",
                                EVENT_TYPE, payload.questionSetId(), ex.getMessage());
                    } else {
                        log.info("Published {} eventId={} questionSet={} to {}",
                                EVENT_TYPE, envelope.eventId(), payload.questionSetId(), topic);
                    }
                });
    }
}
