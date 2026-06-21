package com.matchly.matching.dto;

import com.matchly.matching.domain.SuccessPrediction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Response view of an ML success prediction for a candidate↔job pair. */
public record PredictionView(
        UUID candidateId,
        UUID jobId,
        BigDecimal probability,
        String modelVersion,
        Instant predictedAt
) {

    public static PredictionView from(SuccessPrediction p) {
        return new PredictionView(
                p.getCandidateId(), p.getJobId(), p.getProbability(),
                p.getModelVersion(), p.getPredictedAt());
    }
}
