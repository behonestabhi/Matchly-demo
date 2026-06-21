package com.matchly.candidate.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * DTOs for the AI service internal API ({@code /internal/parse}, {@code /internal/embed}).
 * Field names match the AI service's pydantic models (camelCase JSON).
 * Responses are tolerant of unknown fields so the AI service can evolve.
 */
public final class AiDtos {

    private AiDtos() {
    }

    // ---- /internal/parse -------------------------------------------------- //

    /** Request: raw text OR base64-encoded file (+ mime type). */
    public record ParseRequest(String text, String fileBase64, String mimeType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Experience(String company, String title, String startDate,
                             String endDate, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Education(String institution, String degree, String field,
                            String level, Integer startYear, Integer endYear) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Project(String name, String description, List<String> techStack) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Certification(String name, String issuer, String issuedAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ParseResponse(
            String fullName,
            String email,
            String phone,
            Double totalExpYrs,
            List<String> skills,
            List<Experience> experiences,
            List<Education> education,
            List<Project> projects,
            List<Certification> certifications,
            String rawText) {
    }

    // ---- /internal/embed -------------------------------------------------- //

    /** Request: owner identity + text to embed. {@code ownerType} is RESUME|JOB. */
    public record EmbedRequest(String ownerType, String ownerId, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbedResponse(String embeddingRef, Integer dim, String model) {
    }
}
