package com.matchly.interview.service;

import com.matchly.interview.client.ai.AiInterviewResponse;
import com.matchly.interview.domain.Question;
import com.matchly.interview.domain.QuestionCategory;
import com.matchly.interview.domain.QuestionSet;
import com.matchly.interview.domain.QuestionSetStatus;
import com.matchly.interview.repository.QuestionSetRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional persistence of generation results. Kept in a separate bean so
 * the {@code @Transactional} boundary is honoured by the Spring proxy — the
 * async worker calls these as cross-bean (proxied) invocations.
 */
@Service
public class QuestionSetPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(QuestionSetPersistenceService.class);
    private static final String PROMPT_VERSION = "interview-v1";
    private static final int MAX_ERROR_LEN = 1000;

    private final QuestionSetRepository questionSetRepository;

    public QuestionSetPersistenceService(QuestionSetRepository questionSetRepository) {
        this.questionSetRepository = questionSetRepository;
    }

    /** Persist generated questions and flip the set to READY. */
    @Transactional
    public void markReady(UUID questionSetId, AiInterviewResponse response) {
        QuestionSet set = questionSetRepository.findById(questionSetId).orElse(null);
        if (set == null) {
            log.warn("Question set {} vanished before persistence", questionSetId);
            return;
        }
        int ordinal = 0;
        for (AiInterviewResponse.AiQuestion q : response.questions()) {
            set.addQuestion(new Question(
                    UUID.randomUUID(),
                    parseCategory(q.category()),
                    q.question(),
                    q.suggestedAnswer(),
                    q.difficulty(),
                    ordinal++));
        }
        set.setLlmModel(response.llmModel());
        set.setPromptVersion(PROMPT_VERSION);
        set.setStatus(QuestionSetStatus.READY);
        questionSetRepository.save(set);
    }

    /** Flip the set to FAILED with a (truncated) error detail. */
    @Transactional
    public void markFailed(UUID questionSetId, String detail) {
        questionSetRepository.findById(questionSetId).ifPresent(set -> {
            set.setStatus(QuestionSetStatus.FAILED);
            set.setErrorDetail(truncate(detail));
            questionSetRepository.save(set);
        });
    }

    private QuestionCategory parseCategory(String raw) {
        if (raw == null) {
            return QuestionCategory.TECHNICAL;
        }
        try {
            return QuestionCategory.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown question category '{}', defaulting to TECHNICAL", raw);
            return QuestionCategory.TECHNICAL;
        }
    }

    private String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() > MAX_ERROR_LEN ? s.substring(0, MAX_ERROR_LEN) : s;
    }
}
