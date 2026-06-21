package com.matchly.job.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * A required skill on a job, used both in requests (create/update) and in
 * {@link JobResponse}. {@code weight} defaults to 1.0 and {@code required} to
 * true when omitted (applied in the service).
 */
public record RequiredSkillDto(
        @NotBlank String skillName,
        Double weight,
        Boolean required
) {
}
