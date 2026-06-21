package com.matchly.candidate.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

    @Test
    void ofPopulatesEnvelopeFields() {
        Object payload = Map.of("candidateId", "c1", "resumeId", "r1");
        EventEnvelope envelope = EventEnvelope.of("ResumeUploaded", "corr-123", payload);

        assertThat(envelope.eventId()).isNotBlank();
        assertThat(envelope.eventType()).isEqualTo("ResumeUploaded");
        assertThat(envelope.occurredAt()).isNotNull();
        assertThat(envelope.correlationId()).isEqualTo("corr-123");
        assertThat(envelope.version()).isEqualTo(1);
        assertThat(envelope.payload()).isEqualTo(payload);
    }

    @Test
    void ofGeneratesUniqueEventIds() {
        EventEnvelope a = EventEnvelope.of("ResumeParsed", null, Map.of());
        EventEnvelope b = EventEnvelope.of("ResumeParsed", null, Map.of());
        assertThat(a.eventId()).isNotEqualTo(b.eventId());
    }
}
