package com.matchly.matching.client;

import java.util.List;
import java.util.UUID;

/**
 * The slice of job data the matching engine needs, from the job-service
 * ({@code GET /api/v1/jobs/{id}}).
 *
 * @param jobId          job id
 * @param description    JD text for on-the-fly semantic scoring (nullable)
 * @param requiredSkills required skills with weight / must-have flag
 * @param minExpYrs      minimum required experience (nullable)
 * @param maxExpYrs      maximum experience band (nullable)
 * @param educationLevel required education level (nullable)
 * @param embeddingRef   AI-service vector id for the JD (nullable)
 */
public record JobData(
        UUID jobId,
        String description,
        List<RequiredSkill> requiredSkills,
        Double minExpYrs,
        Double maxExpYrs,
        String educationLevel,
        UUID embeddingRef) {

    /** A required skill on a job posting. */
    public record RequiredSkill(String skillName, Double weight, Boolean required) {
    }
}
