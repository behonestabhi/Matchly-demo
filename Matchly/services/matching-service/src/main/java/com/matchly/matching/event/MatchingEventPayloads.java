package com.matchly.matching.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Payload shapes for matching events (see {@code libs/schemas/events.md}). */
public final class MatchingEventPayloads {

    private MatchingEventPayloads() {
    }

    /** Inbound {@code ApplicationSubmitted} payload: {@code { applicationId, candidateId, jobId }}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApplicationSubmitted(UUID applicationId, UUID candidateId, UUID jobId) {
    }

    /** Outbound {@code MatchScored} payload: {@code { candidateId, jobId, finalScore, modelVersion }}. */
    public record MatchScored(UUID candidateId, UUID jobId, BigDecimal finalScore, String modelVersion) {
    }

    /** Outbound {@code SkillGapComputed} payload: {@code { candidateId, jobId, missingSkills[] }}. */
    public record SkillGapComputed(UUID candidateId, UUID jobId, List<String> missingSkills) {
    }
}
