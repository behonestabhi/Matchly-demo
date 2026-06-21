package com.matchly.job.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Request body for {@code POST /api/v1/jobs}. The job starts in {@code DRAFT}. */
public record CreateJobRequest(
        @NotBlank String title,
        @NotBlank String description,
        String location,
        String employmentType,
        Double minExpYrs,
        Double maxExpYrs,
        String educationLevel,
        @Valid List<RequiredSkillDto> requiredSkills
) {
}
