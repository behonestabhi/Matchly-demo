package com.matchly.interview.service;

import com.matchly.interview.domain.QuestionSet;
import com.matchly.interview.dto.CreateInterviewRequest;
import com.matchly.interview.dto.QuestionSetView;
import com.matchly.interview.exception.NotFoundException;
import com.matchly.interview.repository.QuestionSetRepository;
import com.matchly.interview.security.RequestContext;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates interview question-set creation and retrieval.
 *
 * <p>{@link #create} persists a {@code GENERATING} set synchronously and then
 * kicks off async generation, so the controller can return 202 immediately.
 */
@Service
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private final QuestionSetRepository questionSetRepository;
    private final QuestionGenerationService generationService;

    public InterviewService(QuestionSetRepository questionSetRepository,
                            QuestionGenerationService generationService) {
        this.questionSetRepository = questionSetRepository;
        this.generationService = generationService;
    }

    /**
     * Create a GENERATING question set and trigger async generation.
     *
     * @return the persisted (GENERATING) set id.
     */
    @Transactional
    public UUID create(CreateInterviewRequest request) {
        QuestionSet set = new QuestionSet(UUID.randomUUID(), request.candidateId(), request.jobId());
        questionSetRepository.save(set);
        log.info("Created question set {} (GENERATING) for candidate={} job={}",
                set.getId(), request.candidateId(), request.jobId());

        // Capture the correlation id now; the async thread has its own request scope.
        String correlationId = RequestContext.correlationId();
        generationService.generateAsync(set.getId(), request, correlationId);

        return set.getId();
    }

    @Transactional(readOnly = true)
    public QuestionSetView get(UUID id) {
        QuestionSet set = questionSetRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Question set not found: " + id));
        // Touch lazy collection inside the transaction so the view can map it.
        set.getQuestions().size();
        return QuestionSetView.from(set);
    }

    @Transactional(readOnly = true)
    public List<QuestionSetView> findByPair(UUID candidateId, UUID jobId) {
        List<QuestionSet> sets;
        if (candidateId != null && jobId != null) {
            sets = questionSetRepository.findByCandidateIdAndJobIdOrderByCreatedAtDesc(candidateId, jobId);
        } else if (candidateId != null) {
            sets = questionSetRepository.findByCandidateIdOrderByCreatedAtDesc(candidateId);
        } else if (jobId != null) {
            sets = questionSetRepository.findByJobIdOrderByCreatedAtDesc(jobId);
        } else {
            sets = questionSetRepository.findAll();
        }
        return sets.stream()
                .peek(s -> s.getQuestions().size())
                .map(QuestionSetView::from)
                .toList();
    }
}
