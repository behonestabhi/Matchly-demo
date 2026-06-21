package com.matchly.candidate.repository;

import com.matchly.candidate.domain.CandidateSkill;
import com.matchly.candidate.domain.CandidateSkillId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, CandidateSkillId> {

    List<CandidateSkill> findByIdCandidateId(UUID candidateId);
}
