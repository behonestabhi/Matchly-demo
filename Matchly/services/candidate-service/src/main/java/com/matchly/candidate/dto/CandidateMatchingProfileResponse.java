package com.matchly.candidate.dto;

import java.util.List;
import java.util.UUID;

/**
 * The slice of candidate data the matching service needs, exposed on an
 * in-cluster-only path ({@code /internal/...}, not routed by the gateway).
 * Assembled from the candidate's primary resume + parsed data + skills.
 *
 * <p>Fields may be null/empty when the candidate has no parsed resume yet —
 * matching tolerates this and scores on whatever is present.
 */
public record CandidateMatchingProfileResponse(
        UUID candidateId,
        List<String> skills,
        Double totalExpYrs,
        String educationLevel,
        UUID embeddingRef,
        String resumeText
) {
}
