package com.matchly.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchly.application.config.KafkaTopicsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes application domain events to the {@code application.events} topic,
 * keyed by {@code applicationId} (preserves per-application ordering, per
 * DATA_MODELS §9). Events are JSON-serialized {@link EventEnvelope}s.
 */
@Component
public class ApplicationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ApplicationEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicsProperties topics;

    public ApplicationEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                     ObjectMapper objectMapper,
                                     KafkaTopicsProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topics = topics;
    }

    /**
     * Serialize and publish an event. Publishing failures are logged but never
     * propagated — the core write has already been committed and the API should
     * still succeed (fail-soft on the async path).
     *
     * @param key       Kafka partition key (the applicationId)
     * @param eventType e.g. {@code ApplicationSubmitted}
     * @param payload   event payload object
     * @param correlationId tracing id forwarded from the gateway (nullable)
     */
    public void publish(String key, String eventType, Object payload, String correlationId) {
        EventEnvelope envelope = EventEnvelope.of(eventType, correlationId, payload);
        try {
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(topics.getApplicationEvents(), key, json);
            log.debug("Published {} to {} key={}", eventType, topics.getApplicationEvents(), key);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} event for key={}", eventType, key, e);
        } catch (Exception e) {
            log.error("Failed to publish {} event for key={}", eventType, key, e);
        }
    }
}
