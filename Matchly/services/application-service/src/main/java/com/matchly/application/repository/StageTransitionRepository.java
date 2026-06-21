package com.matchly.application.repository;

import com.matchly.application.domain.StageTransition;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageTransitionRepository extends JpaRepository<StageTransition, UUID> {
}
