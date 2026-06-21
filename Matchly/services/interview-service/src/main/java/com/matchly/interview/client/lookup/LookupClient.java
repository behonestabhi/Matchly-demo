package com.matchly.interview.client.lookup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Optionally enriches a generation request with resume text / JD / skills by
 * calling candidate-service and job-service. Both URLs are configurable and may
 * be blank (disabled). Any failure degrades gracefully to empty values — the AI
 * service still produces useful questions from the skills/JD it does have.
 */
@Component
public class LookupClient {

    private static final Logger log = LoggerFactory.getLogger(LookupClient.class);

    private final RestClient lookupRestClient;
    private final String candidateServiceUrl;
    private final String jobServiceUrl;

    public LookupClient(@Qualifier("lookupRestClient") RestClient lookupRestClient,
                        @Value("${candidate.service.url:}") String candidateServiceUrl,
                        @Value("${job.service.url:}") String jobServiceUrl) {
        this.lookupRestClient = lookupRestClient;
        this.candidateServiceUrl = candidateServiceUrl;
        this.jobServiceUrl = jobServiceUrl;
    }

    /** Best-effort candidate resume lookup; never throws. */
    public CandidateInfo fetchCandidate(UUID candidateId) {
        if (!StringUtils.hasText(candidateServiceUrl) || candidateId == null) {
            return CandidateInfo.empty();
        }
        try {
            CandidateInfo info = lookupRestClient.get()
                    .uri(candidateServiceUrl + "/api/v1/candidates/{id}", candidateId)
                    .retrieve()
                    .body(CandidateInfo.class);
            return info == null ? CandidateInfo.empty() : info;
        } catch (Exception e) {
            log.warn("Candidate lookup failed for {} (continuing without): {}", candidateId, e.getMessage());
            return CandidateInfo.empty();
        }
    }

    /** Best-effort job (JD) lookup; never throws. */
    public JobInfo fetchJob(UUID jobId) {
        if (!StringUtils.hasText(jobServiceUrl) || jobId == null) {
            return JobInfo.empty();
        }
        try {
            JobInfo info = lookupRestClient.get()
                    .uri(jobServiceUrl + "/api/v1/jobs/{id}", jobId)
                    .retrieve()
                    .body(JobInfo.class);
            return info == null ? JobInfo.empty() : info;
        } catch (Exception e) {
            log.warn("Job lookup failed for {} (continuing without): {}", jobId, e.getMessage());
            return JobInfo.empty();
        }
    }

    /** Subset of candidate-service response we care about. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CandidateInfo(String resumeText, List<String> skills) {
        public static CandidateInfo empty() {
            return new CandidateInfo(null, List.of());
        }

        public List<String> skillsOrEmpty() {
            return skills == null ? List.of() : skills;
        }
    }

    /** Subset of job-service response we care about. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record JobInfo(String description) {
        public static JobInfo empty() {
            return new JobInfo(null);
        }
    }
}
