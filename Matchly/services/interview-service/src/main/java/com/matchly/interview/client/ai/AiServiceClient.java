package com.matchly.interview.client.ai;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin client over the AI service's interview-generation endpoint. Wraps the
 * call in a try/catch and surfaces failures as {@link AiServiceException} so the
 * async generator can mark the set FAILED rather than crashing the worker.
 */
@Component
public class AiServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestClient aiRestClient;

    public AiServiceClient(@Qualifier("aiRestClient") RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

    /** Thrown when the AI service call fails or returns an empty/invalid body. */
    public static class AiServiceException extends RuntimeException {
        public AiServiceException(String message, Throwable cause) {
            super(message, cause);
        }

        public AiServiceException(String message) {
            super(message);
        }
    }

    /**
     * Call {@code POST /internal/interview/generate}.
     *
     * @throws AiServiceException on transport error, non-2xx, or empty body.
     */
    public AiInterviewResponse generate(AiInterviewRequest request) {
        try {
            AiInterviewResponse response = aiRestClient.post()
                    .uri("/internal/interview/generate")
                    .body(request)
                    .retrieve()
                    .body(AiInterviewResponse.class);

            if (response == null || response.questions() == null || response.questions().isEmpty()) {
                throw new AiServiceException("AI service returned no questions");
            }
            return response;
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI interview generation call failed: {}", e.getMessage());
            throw new AiServiceException("AI interview generation failed: " + e.getMessage(), e);
        }
    }

    /** Default per-category counts when the caller does not specify any. */
    public static AiInterviewRequest.Counts defaultCounts() {
        return new AiInterviewRequest.Counts(3, 2, 2);
    }

    /** Null-safe skills helper. */
    public static List<String> safeSkills(List<String> skills) {
        return skills == null ? List.of() : skills;
    }
}
