package com.matchly.matching.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Request/response DTOs for the AI service internal endpoints. Field names match
 * the AI service's pydantic models exactly (see {@code ai-service/app/schemas.py}).
 * Responses tolerate unknown fields so the AI service can evolve independently.
 */
public final class AiDtos {

    private AiDtos() {
    }

    /**
     * Request body for {@code POST /internal/score}. Provide embedding refs (when
     * available) and/or texts for on-the-fly semantic scoring, plus skills and
     * experience/education hints for the explainable components.
     */
    public record ScoreRequest(
            String candidateText,
            String jobText,
            String candidateEmbeddingRef,
            String jobEmbeddingRef,
            List<String> candidateSkills,
            List<String> jobRequiredSkills,
            Double expYrs,
            Double minExpYrs,
            Double maxExpYrs,
            String candidateEduLevel,
            String jobEduLevel) {
    }

    /** Response from {@code POST /internal/score} (ARCHITECTURE §6.1). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScoreResponse(
            BigDecimal finalScore,
            BigDecimal semantic,
            BigDecimal skillOverlap,
            BigDecimal experienceFit,
            BigDecimal educationFit,
            List<String> matchedSkills,
            List<String> missingSkills,
            String modelVersion) {
    }

    /** Request body for {@code POST /internal/skill-gap}. */
    public record SkillGapRequest(
            List<String> candidateSkills,
            List<String> jobRequiredSkills,
            boolean generateRoadmap) {
    }

    /**
     * Response from {@code POST /internal/skill-gap}: missing skills (+ optional
     * roadmap). {@code roadmap} is kept as a raw {@link JsonNode} since matching
     * persists it verbatim as JSONB.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SkillGapResponse(
            List<String> missingSkills,
            List<String> matchedSkills,
            JsonNode roadmap) {
    }

    /** Request body for {@code POST /internal/predict}: {@code { features }}. */
    public record PredictRequest(Map<String, Object> features) {
    }

    /** Response from {@code POST /internal/predict}: {@code { probability, modelVersion }}. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PredictResponse(
            BigDecimal probability,
            String modelVersion) {
    }
}
