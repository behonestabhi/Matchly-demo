package com.matchly.interview.repository;

import com.matchly.interview.domain.QuestionSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionSetRepository extends JpaRepository<QuestionSet, UUID> {

    List<QuestionSet> findByCandidateIdAndJobIdOrderByCreatedAtDesc(UUID candidateId, UUID jobId);

    List<QuestionSet> findByCandidateIdOrderByCreatedAtDesc(UUID candidateId);

    List<QuestionSet> findByJobIdOrderByCreatedAtDesc(UUID jobId);
}
