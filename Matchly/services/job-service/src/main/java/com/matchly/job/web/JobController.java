package com.matchly.job.web;

import com.matchly.job.domain.JobStatus;
import com.matchly.job.dto.CreateJobRequest;
import com.matchly.job.dto.JobResponse;
import com.matchly.job.dto.PageResponse;
import com.matchly.job.dto.UpdateJobRequest;
import com.matchly.job.exception.BadRequestException;
import com.matchly.job.security.CurrentUserResolver;
import com.matchly.job.service.JobService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API for job postings (API_CONTRACTS — Job Service). */
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;
    private final CurrentUserResolver currentUser;

    public JobController(JobService jobService, CurrentUserResolver currentUser) {
        this.jobService = jobService;
        this.currentUser = currentUser;
    }

    /** Create a job in DRAFT. The recruiter is taken from the gateway identity. */
    @PostMapping
    public ResponseEntity<JobResponse> create(@Valid @RequestBody CreateJobRequest request,
                                              HttpServletRequest http) {
        UUID recruiterId = currentUser.requireUserId(http);
        JobResponse created = jobService.createJob(recruiterId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public JobResponse update(@PathVariable UUID id,
                              @Valid @RequestBody UpdateJobRequest request) {
        return jobService.updateJob(id, request);
    }

    /** Open the job and emit {@code JobPosted}. */
    @PostMapping("/{id}/publish")
    public JobResponse publish(@PathVariable UUID id, HttpServletRequest http) {
        currentUser.requireUserId(http);
        return jobService.publishJob(id, currentUser.correlationId(http));
    }

    /** Close the job and emit {@code JobClosed}. */
    @PostMapping("/{id}/close")
    public JobResponse close(@PathVariable UUID id, HttpServletRequest http) {
        currentUser.requireUserId(http);
        return jobService.closeJob(id, currentUser.correlationId(http));
    }

    /**
     * List/search jobs. Defaults to {@code OPEN} postings; pass {@code status} to
     * override (DRAFT|OPEN|CLOSED). Filters: {@code skill}, {@code location},
     * {@code minExp}. Paginated via {@code page}/{@code size}.
     */
    @GetMapping
    public PageResponse<JobResponse> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double minExp,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        JobStatus statusFilter = parseStatus(status);
        return jobService.search(statusFilter, skill, location, minExp, page, size);
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable UUID id) {
        return jobService.getJob(id);
    }

    /** Default to OPEN when no status is supplied; reject an unknown status value. */
    private JobStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return JobStatus.OPEN;
        }
        try {
            return JobStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown status: " + status + " (expected DRAFT|OPEN|CLOSED)");
        }
    }
}
