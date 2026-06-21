package com.matchly.interview.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Response from {@code POST /internal/interview/generate}. Mirrors the AI
 * service's {@code InterviewResponse}: {@code {questions:[...], llmModel}}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiInterviewResponse(
        List<AiQuestion> questions,
        String llmModel) {

    /** One generated question ({@code InterviewQuestion} in the AI service). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AiQuestion(
            String category,
            String question,
            String suggestedAnswer,
            String difficulty) {
    }
}
