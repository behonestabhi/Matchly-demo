package com.matchly.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Kafka topic names, bound from {@code matchly.kafka.topics.*}. Env-overridable.
 */
@Component
@ConfigurationProperties(prefix = "matchly.kafka.topics")
public class KafkaTopicsProperties {

    /** Topic this service produces application domain events to. */
    private String applicationEvents = "application.events";

    public String getApplicationEvents() {
        return applicationEvents;
    }

    public void setApplicationEvents(String applicationEvents) {
        this.applicationEvents = applicationEvents;
    }
}
