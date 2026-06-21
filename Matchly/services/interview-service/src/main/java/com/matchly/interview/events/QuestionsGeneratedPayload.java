package com.matchly.interview.events;

import java.util.UUID;

/**
 * Payload of the {@code QuestionsGenerated} event on {@code interview.events}
 * (DATA_MODELS §9 / libs/schemas/events.md): {@code {candidateId, jobId, questionSetId}}.
 */
public record QuestionsGeneratedPayload(UUID candidateId, UUID jobId, UUID questionSetId) {
}
