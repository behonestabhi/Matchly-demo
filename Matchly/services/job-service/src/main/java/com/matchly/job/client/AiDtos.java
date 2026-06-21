package com.matchly.job.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTOs for the AI service internal API. The job service only needs
 * {@code /internal/embed} (to embed a job description on publish). Field names
 * match the AI service's pydantic models (camelCase JSON).
 */
public final class AiDtos {

    private AiDtos() {
    }

    /** Request: owner identity + text to embed. {@code ownerType} is RESUME|JOB. */
    public record EmbedRequest(String ownerType, String ownerId, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbedResponse(String embeddingRef, Integer dim, String model) {
    }
}
