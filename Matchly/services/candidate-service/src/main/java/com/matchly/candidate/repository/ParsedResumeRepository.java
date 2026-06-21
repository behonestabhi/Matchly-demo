package com.matchly.candidate.repository;

import com.matchly.candidate.domain.ParsedResume;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParsedResumeRepository extends JpaRepository<ParsedResume, UUID> {

    Optional<ParsedResume> findByResumeId(UUID resumeId);
}
