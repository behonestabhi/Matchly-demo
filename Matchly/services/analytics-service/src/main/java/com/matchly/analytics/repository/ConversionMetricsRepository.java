package com.matchly.analytics.repository;

import com.matchly.analytics.domain.ConversionMetrics;
import com.matchly.analytics.domain.ConversionMetricsId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversionMetricsRepository extends JpaRepository<ConversionMetrics, ConversionMetricsId> {

    List<ConversionMetrics> findByJobIdOrderByDayAsc(UUID jobId);
}
