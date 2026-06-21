package com.matchly.interview.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

    @Test
    void createPopulatesEnvelopeFieldsAndUtcTimestamp() {
        UUID candidateId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID setId = UUID.randomUUID();
        QuestionsGeneratedPayload payload = new QuestionsGeneratedPayload(candidateId, jobId, setId);

        EventEnvelope<QuestionsGeneratedPayload> envelope =
                EventEnvelope.create("QuestionsGenerated", "corr-1", payload);

        assertThat(envelope.eventId()).isNotBlank();
        assertThat(envelope.eventType()).isEqualTo("QuestionsGenerated");
        assertThat(envelope.version()).isEqualTo(1);
        assertThat(envelope.correlationId()).isEqualTo("corr-1");
        assertThat(envelope.payload()).isEqualTo(payload);
        // occurredAt must be parseable ISO-8601 instant (UTC).
        assertThat(Instant.parse(envelope.occurredAt())).isNotNull();
    }
}
