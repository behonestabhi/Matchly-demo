package com.matchly.matching.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchly.matching.config.MatchingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes matching domain events to {@code matching.events}, keyed by
 * {@code candidateId} (per DATA_MODELS §9). Publish failures are logged, not
 * thrown (fail-soft on the async path).
 */
@Component
public class MatchingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(MatchingEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final MatchingProperties props;

    public MatchingEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper,
                                  MatchingProperties props) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.props = props;
    }

    public void publish(String key, String eventType, Object payload, String correlationId) {
        EventEnvelope envelope = EventEnvelope.of(eventType, correlationId, payload);
        try {
            String json = objectMapper.writeValueAsString(envelope);
            kafkaTemplate.send(props.getKafka().getTopics().getMatchingEvents(), key, json);
            log.debug("Published {} to {} key={}", eventType,
                    props.getKafka().getTopics().getMatchingEvents(), key);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} event for key={}", eventType, key, e);
        } catch (Exception e) {
            log.error("Failed to publish {} event for key={}", eventType, key, e);
        }
    }
}
