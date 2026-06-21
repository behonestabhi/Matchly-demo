package com.matchly.matching.dto;

import com.matchly.matching.domain.MatchScore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Response view of a persisted match score with its explainable breakdown. */
public record MatchScoreView(
        UUID candidateId,
        UUID jobId,
        BigDecimal finalScore,
        Breakdown breakdown,
        String modelVersion,
        Instant scoredAt
) {

    public record Breakdown(
            BigDecimal semantic,
            BigDecimal skillOverlap,
            BigDecimal experienceFit,
            BigDecimal educationFit) {
    }

    public static MatchScoreView from(MatchScore s) {
        return new MatchScoreView(
                s.getCandidateId(),
                s.getJobId(),
                s.getFinalScore(),
                new Breakdown(s.getSemanticScore(), s.getSkillOverlap(),
                        s.getExperienceFit(), s.getEducationFit()),
                s.getModelVersion(),
                s.getScoredAt());
    }
}
