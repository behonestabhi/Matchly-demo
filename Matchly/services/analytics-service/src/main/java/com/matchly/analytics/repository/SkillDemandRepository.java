package com.matchly.analytics.repository;

import com.matchly.analytics.domain.SkillDemand;
import com.matchly.analytics.domain.SkillDemandId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SkillDemandRepository extends JpaRepository<SkillDemand, SkillDemandId> {

    /** Aggregate job counts per skill name within a date window (most in-demand first). */
    @Query("""
            SELECT s.skillName AS skillName, SUM(s.jobCount) AS total
            FROM SkillDemand s
            WHERE s.day >= :from
            GROUP BY s.skillName
            ORDER BY SUM(s.jobCount) DESC
            """)
    List<SkillCount> aggregateSince(@Param("from") LocalDate from);

    /** Projection for the in-demand query. */
    interface SkillCount {
        String getSkillName();

        Long getTotal();
    }
}
