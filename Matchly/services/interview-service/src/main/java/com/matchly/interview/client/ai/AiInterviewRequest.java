package com.matchly.interview.client.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Request body for {@code POST {AI_SERVICE_URL}/internal/interview/generate}.
 * Mirrors the AI service's {@code InterviewRequest} (ai-service/app/schemas.py).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiInterviewRequest(
        String resumeText,
        String jobDescription,
        List<String> skills,
        Counts counts) {

    /** Per-category question counts ({@code QuestionCounts} in the AI service). */
    public record Counts(int technical, int behavioral, int scenario) {
    }
}
