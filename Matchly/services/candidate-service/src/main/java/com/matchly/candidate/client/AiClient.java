package com.matchly.candidate.client;

import com.matchly.candidate.client.AiDtos.EmbedRequest;
import com.matchly.candidate.client.AiDtos.EmbedResponse;
import com.matchly.candidate.client.AiDtos.ParseRequest;
import com.matchly.candidate.client.AiDtos.ParseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin client over the AI service internal endpoints. Calls are best-effort:
 * failures are logged and surfaced as {@link AiClientException} so the caller
 * can mark a resume {@code FAILED} rather than crashing the parse flow.
 */
@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    private final RestClient aiRestClient;

    public AiClient(@Qualifier("aiRestClient") RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

    /** POST {@code /internal/parse}. */
    public ParseResponse parse(ParseRequest request) {
        try {
            return aiRestClient.post()
                    .uri("/internal/parse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ParseResponse.class);
        } catch (RestClientException ex) {
            log.warn("AI /internal/parse call failed: {}", ex.getMessage());
            throw new AiClientException("Resume parsing failed (AI service unavailable or errored)", ex);
        }
    }

    /** POST {@code /internal/embed}. */
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

    /** Raised when an AI call fails; lets the parse flow set parse_status=FAILED. */
    public static class AiClientException extends RuntimeException {
        public AiClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
