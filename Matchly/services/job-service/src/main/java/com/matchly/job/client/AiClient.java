package com.matchly.job.client;

import com.matchly.job.client.AiDtos.EmbedRequest;
import com.matchly.job.client.AiDtos.EmbedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin client over the AI service. Calls are best-effort: a failure is logged
 * and surfaced as {@link AiClientException} so the caller can publish the job
 * without an embedding rather than failing the request.
 */
@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    private final RestClient aiRestClient;

    public AiClient(@Qualifier("aiRestClient") RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

    /** POST {@code /internal/embed} — embed the job description text. */
    public EmbedResponse embed(EmbedRequest request) {
        try {
            return aiRestClient.post()
                    .uri("/internal/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(EmbedResponse.class);
        } catch (RestClientException ex) {
            log.warn("AI /internal/embed call failed: {}", ex.getMessage());
            throw new AiClientException("Embedding failed (AI service unavailable or errored)", ex);
        }
    }

    /** Raised when an AI call fails; lets the publish flow continue without an embedding. */
    public static class AiClientException extends RuntimeException {
        public AiClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
