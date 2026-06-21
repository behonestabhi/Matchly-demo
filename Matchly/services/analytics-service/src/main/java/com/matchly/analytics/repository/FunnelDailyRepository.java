package com.matchly.analytics.repository;

import com.matchly.analytics.domain.FunnelDaily;
import com.matchly.analytics.domain.FunnelDailyId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FunnelDailyRepository extends JpaRepository<FunnelDaily, FunnelDailyId> {

    List<FunnelDaily> findByJobIdOrderByDayAsc(UUID jobId);
}
