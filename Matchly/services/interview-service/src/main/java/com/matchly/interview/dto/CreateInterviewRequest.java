package com.matchly.interview.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Body for {@code POST /api/v1/interviews}. {@code candidateId} and {@code jobId}
 * are required; resume/JD/skills are optional hints — when absent the service may
 * fetch them from candidate/job services (graceful fallback to empty).
 */
public record CreateInterviewRequest(
        @NotNull(message = "candidateId is required") UUID candidateId,
        @NotNull(message = "jobId is required") UUID jobId,
        String resumeText,
        String jobDescription,
        List<String> skills) {
}
