package com.matchly.candidate.repository;

import com.matchly.candidate.domain.Candidate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    Optional<Candidate> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
