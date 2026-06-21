package com.matchly.matching.repository;

import com.matchly.matching.domain.SuccessPrediction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SuccessPredictionRepository extends JpaRepository<SuccessPrediction, UUID> {

    Optional<SuccessPrediction> findFirstByCandidateIdAndJobIdOrderByPredictedAtDesc(UUID candidateId, UUID jobId);
}
