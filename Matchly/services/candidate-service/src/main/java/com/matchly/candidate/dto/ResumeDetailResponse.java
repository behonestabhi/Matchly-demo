package com.matchly.candidate.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response for {@code GET /resumes/{id}}. Carries the parse status always, and
 * the structured data once {@code parseStatus == PARSED} (null/empty before that).
 *
 * <pre>{ "id":"...", "parseStatus":"PARSED",
 *        "candidate":{ "fullName":"...", "email":"...", "totalExpYrs":4.5 },
 *        "skills":[...], "experiences":[...], "education":[...],
 *        "projects":[...], "certifications":[...] }</pre>
 */
public record ResumeDetailResponse(
        UUID id,
        UUID candidateId,
        String fileName,
        String mimeType,
        String parseStatus,
        UUID embeddingRef,
        ParsedCandidate candidate,
        List<String> skills,
        List<ExperienceDto> experiences,
        List<EducationDto> education,
        List<ProjectDto> projects,
        List<CertificationDto> certifications) {

    public record ParsedCandidate(String fullName, String email, Double totalExpYrs) {
    }

    public record ExperienceDto(String company, String title,
                                LocalDate startDate, LocalDate endDate, String description) {
    }

    public record EducationDto(String institution, String degree, String field,
                               String level, Integer startYear, Integer endYear) {
    }

    public record ProjectDto(String name, String description, List<String> techStack) {
    }

    public record CertificationDto(String name, String issuer, LocalDate issuedAt) {
    }
}
