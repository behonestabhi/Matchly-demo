package com.matchly.candidate.repository;

import com.matchly.candidate.domain.Experience;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceRepository extends JpaRepository<Experience, UUID> {

    List<Experience> findByResumeId(UUID resumeId);
}
