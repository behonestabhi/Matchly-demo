package com.matchly.candidate.repository;

import com.matchly.candidate.domain.Project;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByResumeId(UUID resumeId);
}
