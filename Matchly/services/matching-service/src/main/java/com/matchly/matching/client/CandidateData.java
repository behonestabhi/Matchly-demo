package com.matchly.matching.client;

import java.util.List;
import java.util.UUID;

/**
 * The slice of candidate data the matching engine needs, fetched from the
 * candidate-service ({@code GET /internal/candidates/{id}/matching-profile}).
 * Fields may be null/empty if the candidate has no parsed resume yet.
 *
 * @param candidateId    candidate id
 * @param skills         resume skills (normalized strings)
 * @param totalExpYrs    total years of experience (nullable)
 * @param educationLevel highest education level (e.g. BACHELOR, MASTER) (nullable)
 * @param embeddingRef   AI-service vector id for the resume (nullable)
 * @param resumeText     (capped) resume text for on-the-fly semantic scoring (nullable)
 */
public record CandidateData(
        UUID candidateId,
        List<String> skills,
        Double totalExpYrs,
        String educationLevel,
        UUID embeddingRef,
        String resumeText) {
}
