package com.matchly.candidate.repository;

import com.matchly.candidate.domain.Resume;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    List<Resume> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);
}
