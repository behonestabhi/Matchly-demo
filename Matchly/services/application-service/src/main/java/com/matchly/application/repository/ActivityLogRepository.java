package com.matchly.application.repository;

import com.matchly.application.domain.ActivityLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    List<ActivityLog> findByApplicationIdOrderByOccurredAtAsc(UUID applicationId);
}
