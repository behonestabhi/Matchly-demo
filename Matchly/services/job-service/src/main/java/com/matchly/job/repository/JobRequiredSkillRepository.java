package com.matchly.job.repository;

import com.matchly.job.domain.JobRequiredSkill;
import com.matchly.job.domain.JobRequiredSkillId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface JobRequiredSkillRepository
        extends JpaRepository<JobRequiredSkill, JobRequiredSkillId> {

    List<JobRequiredSkill> findByIdJobId(UUID jobId);

    @Transactional
    void deleteByIdJobId(UUID jobId);
}
