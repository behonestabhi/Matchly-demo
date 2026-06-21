package com.matchly.matching.repository;

import com.matchly.matching.domain.SkillGap;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillGapRepository extends JpaRepository<SkillGap, UUID> {

    Optional<SkillGap> findByCandidateIdAndJobId(UUID candidateId, UUID jobId);
}
