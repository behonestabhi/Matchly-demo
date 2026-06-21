package com.matchly.interview.dto;

import com.matchly.interview.domain.QuestionSet;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Full question-set view for the GET endpoints (set + questions + status). */
public record QuestionSetView(
        UUID id,
        UUID candidateId,
        UUID jobId,
        String status,
        String llmModel,
        String promptVersion,
        String errorDetail,
        Instant createdAt,
        Instant updatedAt,
        List<QuestionView> questions) {

    public static QuestionSetView from(QuestionSet set) {
        List<QuestionView> qs = set.getQuestions().stream()
                .map(QuestionView::from)
                .toList();
        return new QuestionSetView(
                set.getId(),
                set.getCandidateId(),
                set.getJobId(),
                set.getStatus() == null ? null : set.getStatus().name(),
                set.getLlmModel(),
                set.getPromptVersion(),
                set.getErrorDetail(),
                set.getCreatedAt(),
                set.getUpdatedAt(),
                qs);
    }
}
