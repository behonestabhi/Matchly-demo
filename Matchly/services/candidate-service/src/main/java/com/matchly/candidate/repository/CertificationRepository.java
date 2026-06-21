package com.matchly.candidate.repository;

import com.matchly.candidate.domain.Certification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificationRepository extends JpaRepository<Certification, UUID> {

    List<Certification> findByResumeId(UUID resumeId);
}
