package com.matchly.job.dto;

import com.matchly.job.domain.Job;
import com.matchly.job.domain.JobRequiredSkill;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Response view of a job posting and its required skills. */
public record JobResponse(
        UUID id,
        UUID recruiterId,
        String title,
        String description,
        String location,
        String employmentType,
        Double minExpYrs,
        Double maxExpYrs,
        String educationLevel,
        String status,
        UUID embeddingRef,
        Instant publishedAt,
        Instant createdAt,
        List<RequiredSkillDto> requiredSkills
) {

    /** Assemble a response from the job aggregate and its skills. */
    public static JobResponse from(Job job, List<JobRequiredSkill> skills) {
        List<RequiredSkillDto> skillDtos = skills.stream()
                .map(s -> new RequiredSkillDto(s.getSkillName(), s.getWeight(), s.getRequired()))
                .toList();
        return new JobResponse(
                job.getId(),
                job.getRecruiterId(),
                job.getTitle(),
                job.getDescription(),
                job.getLocation(),
                job.getEmploymentType(),
                job.getMinExpYrs(),
                job.getMaxExpYrs(),
                job.getEducationLevel(),
                job.getStatus().name(),
                job.getEmbeddingRef(),
                job.getPublishedAt(),
                job.getCreatedAt(),
                skillDtos);
    }
}
