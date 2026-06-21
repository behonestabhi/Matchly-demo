package com.matchly.matching.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.matchly.matching.exception.ServiceUnavailableException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Fetches the matching slice of a candidate from the candidate-service. */
@Component
public class CandidateClient {

    private static final Logger log = LoggerFactory.getLogger(CandidateClient.class);

    private final RestClient candidateRestClient;

    public CandidateClient(@Qualifier("candidateRestClient") RestClient candidateRestClient) {
        this.candidateRestClient = candidateRestClient;
    }

    /** GET {@code /internal/candidates/{id}/matching-profile}. */
    public CandidateData fetch(UUID candidateId) {
        try {
            MatchingProfile body = candidateRestClient.get()
                    .uri("/internal/candidates/{id}/matching-profile", candidateId)
                    .retrieve()
                    .body(MatchingProfile.class);
            if (body == null) {
                throw new ServiceUnavailableException("Empty candidate matching profile for " + candidateId);
            }
            return new CandidateData(
                    candidateId,
                    body.skills() == null ? List.of() : body.skills(),
                    body.totalExpYrs(),
                    body.educationLevel(),
                    body.embeddingRef(),
                    body.resumeText());
        } catch (RestClientException ex) {
            log.warn("candidate-service call failed for {}: {}", candidateId, ex.getMessage());
            throw new ServiceUnavailableException("candidate-service unavailable for " + candidateId);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MatchingProfile(
            UUID candidateId,
            List<String> skills,
            Double totalExpYrs,
            String educationLevel,
            UUID embeddingRef,
            String resumeText) {
    }
}
