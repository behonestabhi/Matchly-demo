package com.matchly.candidate.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.matchly.candidate.exception.ServiceUnavailableException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Proxy to the Matching service for skill-gap lookups. The candidate-service
 * forwards {@code candidateId}/{@code jobId} and relays the response verbatim
 * (as a {@link JsonNode}) so it need not couple to the matching contract shape.
 * If matching is unavailable, a 503 ProblemDetail is returned to the caller.
 */
@Component
public class MatchingClient {

    private static final Logger log = LoggerFactory.getLogger(MatchingClient.class);

    private final RestClient matchingRestClient;

    public MatchingClient(@Qualifier("matchingRestClient") RestClient matchingRestClient) {
        this.matchingRestClient = matchingRestClient;
    }

    /** GET {@code /api/v1/matching/skill-gap?candidateId=&jobId=}. */
    public JsonNode skillGap(UUID candidateId, UUID jobId, String correlationId) {
        try {
            return matchingRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/matching/skill-gap")
                            .queryParam("candidateId", candidateId)
                            .queryParam("jobId", jobId)
                            .build())
                    .headers(headers -> {
                        if (correlationId != null && !correlationId.isBlank()) {
                            headers.add("X-Correlation-Id", correlationId);
                        }
                    })
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            log.warn("Matching /skill-gap call failed (candidateId={}, jobId={}): {}",
                    candidateId, jobId, ex.getMessage());
            throw new ServiceUnavailableException(
                    "Skill-gap is temporarily unavailable (matching-service unreachable)");
        }
    }
}
