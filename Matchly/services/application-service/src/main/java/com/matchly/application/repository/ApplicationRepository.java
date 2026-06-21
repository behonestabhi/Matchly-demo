package com.matchly.application.repository;

import com.matchly.application.domain.Application;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    List<Application> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);

    List<Application> findByJobIdOrderByCreatedAtAsc(UUID jobId);

    Optional<Application> findByCandidateIdAndJobId(UUID candidateId, UUID jobId);

    boolean existsByCandidateIdAndJobId(UUID candidateId, UUID jobId);
}
