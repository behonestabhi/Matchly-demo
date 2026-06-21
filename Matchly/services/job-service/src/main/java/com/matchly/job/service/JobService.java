package com.matchly.job.service;

import com.matchly.job.client.AiClient;
import com.matchly.job.client.AiDtos.EmbedRequest;
import com.matchly.job.client.AiDtos.EmbedResponse;
import com.matchly.job.domain.Job;
import com.matchly.job.domain.JobRequiredSkill;
import com.matchly.job.domain.JobStatus;
import com.matchly.job.dto.CreateJobRequest;
import com.matchly.job.dto.JobResponse;
import com.matchly.job.dto.PageResponse;
import com.matchly.job.dto.RequiredSkillDto;
import com.matchly.job.dto.UpdateJobRequest;
import com.matchly.job.events.JobEventPublisher;
import com.matchly.job.exception.ConflictException;
import com.matchly.job.exception.NotFoundException;
import com.matchly.job.repository.JobRepository;
import com.matchly.job.repository.JobRequiredSkillRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Job lifecycle, search, and JD embedding orchestration. */
@Service
@Transactional
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final JobRequiredSkillRepository skillRepository;
    private final AiClient aiClient;
    private final JobEventPublisher eventPublisher;

    public JobService(JobRepository jobRepository,
                      JobRequiredSkillRepository skillRepository,
                      AiClient aiClient,
                      JobEventPublisher eventPublisher) {
        this.jobRepository = jobRepository;
        this.skillRepository = skillRepository;
        this.aiClient = aiClient;
        this.eventPublisher = eventPublisher;
    }

    public JobResponse createJob(UUID recruiterId, CreateJobRequest req) {
        Job job = new Job(UUID.randomUUID(), recruiterId, req.title(), req.description());
        job.setLocation(req.location());
        job.setEmploymentType(req.employmentType());
        job.setMinExpYrs(req.minExpYrs());
        job.setMaxExpYrs(req.maxExpYrs());
        job.setEducationLevel(req.educationLevel());
        job.setStatus(JobStatus.DRAFT);
        jobRepository.save(job);

        List<JobRequiredSkill> skills = persistSkills(job.getId(), req.requiredSkills());
        log.info("Created job {} ({} skills) for recruiter {}", job.getId(), skills.size(), recruiterId);
        return JobResponse.from(job, skills);
    }

    public JobResponse updateJob(UUID jobId, UpdateJobRequest req) {
        Job job = requireJob(jobId);
        if (req.title() != null) {
            job.setTitle(req.title());
        }
        if (req.description() != null) {
            job.setDescription(req.description());
        }
        if (req.location() != null) {
            job.setLocation(req.location());
        }
        if (req.employmentType() != null) {
            job.setEmploymentType(req.employmentType());
        }
        if (req.minExpYrs() != null) {
            job.setMinExpYrs(req.minExpYrs());
        }
        if (req.maxExpYrs() != null) {
            job.setMaxExpYrs(req.maxExpYrs());
        }
        if (req.educationLevel() != null) {
            job.setEducationLevel(req.educationLevel());
        }
        jobRepository.save(job);

        List<JobRequiredSkill> skills;
        if (req.requiredSkills() != null) {
            skillRepository.deleteByIdJobId(jobId);
            skills = persistSkills(jobId, req.requiredSkills());
        } else {
            skills = skillRepository.findByIdJobId(jobId);
        }
        return JobResponse.from(job, skills);
    }

    /** Publish a job: embed its JD (best-effort), open it, and emit {@code JobPosted}. */
    public JobResponse publishJob(UUID jobId, String correlationId) {
        Job job = requireJob(jobId);
        if (job.getStatus() == JobStatus.CLOSED) {
            throw new ConflictException("Cannot publish a closed job");
        }
        if (job.getStatus() != JobStatus.OPEN) {
            embedDescription(job);
            job.setStatus(JobStatus.OPEN);
            job.setPublishedAt(Instant.now());
            jobRepository.save(job);
        }
        List<JobRequiredSkill> skills = skillRepository.findByIdJobId(jobId);
        eventPublisher.publishJobPosted(job, skills, correlationId);
        log.info("Published job {}", jobId);
        return JobResponse.from(job, skills);
    }

    public JobResponse closeJob(UUID jobId, String correlationId) {
        Job job = requireJob(jobId);
        if (job.getStatus() != JobStatus.CLOSED) {
            job.setStatus(JobStatus.CLOSED);
            jobRepository.save(job);
            eventPublisher.publishJobClosed(jobId, correlationId);
            log.info("Closed job {}", jobId);
        }
        return JobResponse.from(job, skillRepository.findByIdJobId(jobId));
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        Job job = requireJob(jobId);
        return JobResponse.from(job, skillRepository.findByIdJobId(jobId));
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> search(JobStatus status, String skill, String location,
                                            Double minExp, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Job> jobs = jobRepository.search(status, blankToNull(location), minExp, blankToNull(skill), pageable);
        return PageResponse.of(jobs, j -> JobResponse.from(j, skillRepository.findByIdJobId(j.getId())));
    }

    // ---- helpers --------------------------------------------------------- //

    private void embedDescription(Job job) {
        try {
            String text = job.getTitle() + "\n\n" + job.getDescription();
            EmbedResponse resp = aiClient.embed(new EmbedRequest("JOB", job.getId().toString(), text));
            if (resp != null && resp.embeddingRef() != null) {
                job.setEmbeddingRef(tryUuid(resp.embeddingRef()));
            }
        } catch (AiClient.AiClientException ex) {
            // Fail-soft: publish without an embedding; matching can re-embed later.
            log.warn("JD embedding skipped for job {}: {}", job.getId(), ex.getMessage());
        }
    }

    private List<JobRequiredSkill> persistSkills(UUID jobId, List<RequiredSkillDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        List<JobRequiredSkill> skills = dtos.stream()
                .filter(d -> d.skillName() != null && !d.skillName().isBlank())
                .map(d -> new JobRequiredSkill(
                        jobId,
                        UUID.randomUUID(),
                        d.skillName().trim(),
                        d.weight() != null ? d.weight() : 1.0,
                        d.required() != null ? d.required() : Boolean.TRUE))
                .toList();
        return skillRepository.saveAll(skills);
    }

    private Job requireJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found: " + jobId));
    }

    private static UUID tryUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
