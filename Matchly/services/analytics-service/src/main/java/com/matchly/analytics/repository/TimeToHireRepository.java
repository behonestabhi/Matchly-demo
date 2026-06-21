package com.matchly.analytics.repository;

import com.matchly.analytics.domain.TimeToHire;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimeToHireRepository extends JpaRepository<TimeToHire, UUID> {

    List<TimeToHire> findByHiredAtIsNotNull();

    List<TimeToHire> findByHiredAtBetween(Instant from, Instant to);

    List<TimeToHire> findByJobId(UUID jobId);
}
