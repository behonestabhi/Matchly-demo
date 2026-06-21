package com.matchly.matching.client;

import com.matchly.matching.client.AiDtos.PredictRequest;
import com.matchly.matching.client.AiDtos.PredictResponse;
import com.matchly.matching.client.AiDtos.ScoreRequest;
import com.matchly.matching.client.AiDtos.ScoreResponse;
import com.matchly.matching.client.AiDtos.SkillGapRequest;
import com.matchly.matching.client.AiDtos.SkillGapResponse;
import com.matchly.matching.exception.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Calls the AI service scoring / skill-gap / prediction endpoints. */
@Component
public class AiScoreClient {

    private static final Logger log = LoggerFactory.getLogger(AiScoreClient.class);

    private final RestClient aiRestClient;

    public AiScoreClient(@Qualifier("aiRestClient") RestClient aiRestClient) {
        this.aiRestClient = aiRestClient;
    }

    /** POST {@code /internal/score}. */
    public ScoreResponse score(ScoreRequest request) {
        return post("/internal/score", request, ScoreResponse.class);
    }

    /** POST {@code /internal/skill-gap}. */
    public SkillGapResponse skillGap(SkillGapRequest request) {
        return post("/internal/skill-gap", request, SkillGapResponse.class);
    }

    /** POST {@code /internal/predict}. */
    public PredictResponse predict(PredictRequest request) {
        return post("/internal/predict", request, PredictResponse.class);
    }

    private <T> T post(String uri, Object request, Class<T> responseType) {
        try {
            return aiRestClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException ex) {
            log.warn("AI {} call failed: {}", uri, ex.getMessage());
            throw new ServiceUnavailableException("AI service unavailable (" + uri + ")");
        }
    }
}
