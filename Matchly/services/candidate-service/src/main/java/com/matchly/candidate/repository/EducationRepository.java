package com.matchly.candidate.repository;

import com.matchly.candidate.domain.Education;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EducationRepository extends JpaRepository<Education, UUID> {

    List<Education> findByResumeId(UUID resumeId);
}
