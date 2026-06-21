package com.matchly.job.repository;

import com.matchly.job.domain.Job;
import com.matchly.job.domain.JobStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, UUID> {

    /**
     * Null-tolerant search. Any of {@code status}, {@code location}, {@code minExp}
     * or {@code skill} may be {@code null} to skip that filter. The {@code skill}
     * filter matches against {@code job_required_skills.skill_name}.
     */
    @Query("""
            SELECT j FROM Job j
            WHERE (:status IS NULL OR j.status = :status)
              AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
              AND (:minExp IS NULL OR j.minExpYrs >= :minExp)
              AND (:skill IS NULL OR EXISTS (
                    SELECT 1 FROM JobRequiredSkill s
                    WHERE s.id.jobId = j.id
                      AND LOWER(s.skillName) LIKE LOWER(CONCAT('%', :skill, '%'))))
            """)
    Page<Job> search(@Param("status") JobStatus status,
                     @Param("location") String location,
                     @Param("minExp") Double minExp,
                     @Param("skill") String skill,
                     Pageable pageable);
}
