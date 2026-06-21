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

/** Fetches a job posting from the job-service and maps it to {@link JobData}. */
@Component
public class JobClient {

    private static final Logger log = LoggerFactory.getLogger(JobClient.class);

    private final RestClient jobRestClient;

    public JobClient(@Qualifier("jobRestClient") RestClient jobRestClient) {
        this.jobRestClient = jobRestClient;
    }

    /** GET {@code /api/v1/jobs/{id}}. */
    public JobData fetch(UUID jobId) {
        try {
            JobApiResponse body = jobRestClient.get()
                    .uri("/api/v1/jobs/{id}", jobId)
                    .retrieve()
                    .body(JobApiResponse.class);
            if (body == null) {
                throw new ServiceUnavailableException("Empty job response for " + jobId);
            }
            List<JobData.RequiredSkill> skills = body.requiredSkills() == null ? List.of()
                    : body.requiredSkills().stream()
                            .map(s -> new JobData.RequiredSkill(s.skillName(), s.weight(), s.required()))
                            .toList();
            return new JobData(
                    jobId,
                    body.description(),
                    skills,
                    body.minExpYrs(),
                    body.maxExpYrs(),
                    body.educationLevel(),
                    body.embeddingRef());
        } catch (RestClientException ex) {
            log.warn("job-service call failed for {}: {}", jobId, ex.getMessage());
            throw new ServiceUnavailableException("job-service unavailable for " + jobId);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JobApiResponse(
            UUID id,
            String description,
            Double minExpYrs,
            Double maxExpYrs,
            String educationLevel,
            UUID embeddingRef,
            List<RequiredSkillDto> requiredSkills) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RequiredSkillDto(String skillName, Double weight, Boolean required) {
    }
}
