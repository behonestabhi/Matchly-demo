package com.matchly.matching.repository;

import com.matchly.matching.domain.MatchScore;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchScoreRepository extends JpaRepository<MatchScore, UUID> {

    /** Ranked candidates for a job (highest final_score first). */
    Page<MatchScore> findByJobIdOrderByFinalScoreDesc(UUID jobId, Pageable pageable);

    /** Best-matching jobs for a candidate (highest final_score first). */
    Page<MatchScore> findByCandidateIdOrderByFinalScoreDesc(UUID candidateId, Pageable pageable);

    Optional<MatchScore> findByCandidateIdAndJobIdAndModelVersion(UUID candidateId, UUID jobId, String modelVersion);

    /** Most recent score for a pair regardless of model version. */
    Optional<MatchScore> findFirstByCandidateIdAndJobIdOrderByScoredAtDesc(UUID candidateId, UUID jobId);
}
