package com.matchly.job.dto;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Request body for {@code PUT /api/v1/jobs/{id}}. All fields are optional; a
 * non-null field replaces the stored value. When {@code requiredSkills} is
 * non-null the full skill set is replaced.
 */
public record UpdateJobRequest(
        String title,
        String description,
        String location,
        String employmentType,
        Double minExpYrs,
        Double maxExpYrs,
        String educationLevel,
        @Valid List<RequiredSkillDto> requiredSkills
) {
}
