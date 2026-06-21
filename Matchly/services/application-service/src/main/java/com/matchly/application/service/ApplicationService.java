package com.matchly.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchly.application.domain.ActivityLog;
import com.matchly.application.domain.Application;
import com.matchly.application.domain.Comment;
import com.matchly.application.domain.Stage;
import com.matchly.application.domain.StageTransition;
import com.matchly.application.event.ApplicationEventPayloads.ApplicationSubmitted;
import com.matchly.application.event.ApplicationEventPayloads.StageChanged;
import com.matchly.application.event.ApplicationEventPublisher;
import com.matchly.application.exception.BadRequestException;
import com.matchly.application.exception.ConflictException;
import com.matchly.application.exception.NotFoundException;
import com.matchly.application.repository.ActivityLogRepository;
import com.matchly.application.repository.ApplicationRepository;
import com.matchly.application.repository.CommentRepository;
import com.matchly.application.repository.StageTransitionRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core ATS domain logic: apply to a job, move through the pipeline, comment, and
 * keep an append-only audit trail. Each mutating method records a
 * {@link StageTransition}/{@link ActivityLog} as appropriate and produces the
 * matching Kafka event <em>after</em> the transaction commits successfully.
 */
@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applications;
    private final StageTransitionRepository transitions;
    private final CommentRepository comments;
    private final ActivityLogRepository activity;
    private final ApplicationEventPublisher events;
    private final ObjectMapper objectMapper;

    public ApplicationService(ApplicationRepository applications,
                              StageTransitionRepository transitions,
                              CommentRepository comments,
                              ActivityLogRepository activity,
                              ApplicationEventPublisher events,
                              ObjectMapper objectMapper) {
        this.applications = applications;
        this.transitions = transitions;
        this.comments = comments;
        this.activity = activity;
        this.events = events;
        this.objectMapper = objectMapper;
    }

    // ------------------------------------------------------------------ apply

    /**
     * Apply a candidate to a job. Creates an APPLIED application, writes the
     * initial stage transition and an activity-log entry, then produces
     * {@code ApplicationSubmitted}.
     */
    @Transactional
    public Application apply(UUID candidateId, UUID jobId, String source, String correlationId) {
        if (applications.existsByCandidateIdAndJobId(candidateId, jobId)) {
            throw new ConflictException("Candidate has already applied to this job");
        }

        Application application = new Application(UUID.randomUUID(), candidateId, jobId,
                source == null ? "DIRECT" : source);
        try {
            applications.saveAndFlush(application);
        } catch (DataIntegrityViolationException e) {
            // Lost the race against the UNIQUE (candidate_id, job_id) constraint.
            throw new ConflictException("Candidate has already applied to this job");
        }

        Instant now = Instant.now();
        transitions.save(new StageTransition(UUID.randomUUID(), application.getId(),
                null, Stage.APPLIED, candidateId, "Applied", now));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("candidateId", candidateId.toString());
        payload.put("jobId", jobId.toString());
        payload.put("toStage", Stage.APPLIED.name());
        writeActivity(application.getId(), candidateId, "APPLIED", payload, now);

        // Produced after commit via the wrapping transaction (publish failures are
        // logged, not thrown — see ApplicationEventPublisher).
        events.publish(application.getId().toString(), "ApplicationSubmitted",
                new ApplicationSubmitted(application.getId(), candidateId, jobId), correlationId);

        return application;
    }

    // ----------------------------------------------------------- stage change

    /**
     * Move an application to a new stage, enforcing the allowed-transition graph.
     * Writes a {@link StageTransition} and an activity entry, then produces
     * {@code StageChanged}.
     */
    @Transactional
    public Application changeStage(UUID applicationId, String toStageRaw, String reason,
                                   UUID actorId, String correlationId) {
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found: " + applicationId));

        Stage from = application.getCurrentStage();
        Stage to = Stage.fromString(toStageRaw);

        if (from == to) {
            throw new BadRequestException("Application is already in stage " + to);
        }
        if (!from.canTransitionTo(to)) {
            throw new BadRequestException("Illegal stage transition: " + from + " -> " + to);
        }

        application.setCurrentStage(to);
        applications.save(application);

        Instant now = Instant.now();
        transitions.save(new StageTransition(UUID.randomUUID(), applicationId,
                from, to, actorId, reason, now));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fromStage", from.name());
        payload.put("toStage", to.name());
        payload.put("reason", reason);
        writeActivity(applicationId, actorId, "STAGE_CHANGED", payload, now);

        events.publish(applicationId.toString(), "StageChanged",
                new StageChanged(applicationId, from.name(), to.name(), actorId), correlationId);

        return application;
    }

    // --------------------------------------------------------------- comments

    @Transactional
    public Comment addComment(UUID applicationId, UUID authorId, String body, String visibility) {
        Application application = applications.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found: " + applicationId));

        String vis = (visibility == null || visibility.isBlank()) ? "INTERNAL" : visibility.trim().toUpperCase();
        if (!vis.equals("INTERNAL") && !vis.equals("SHARED")) {
            throw new BadRequestException("visibility must be INTERNAL or SHARED");
        }

        Comment comment = new Comment(UUID.randomUUID(), application.getId(), authorId, body, vis);
        comments.save(comment);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commentId", comment.getId().toString());
        payload.put("visibility", vis);
        writeActivity(applicationId, authorId, "COMMENTED", payload, Instant.now());

        return comment;
    }

    // ------------------------------------------------------------------ reads

    @Transactional(readOnly = true)
    public List<Application> findByCandidate(UUID candidateId) {
        return applications.findByCandidateIdOrderByCreatedAtDesc(candidateId);
    }

    @Transactional(readOnly = true)
    public List<Application> findByJob(UUID jobId) {
        return applications.findByJobIdOrderByCreatedAtAsc(jobId);
    }

    @Transactional(readOnly = true)
    public List<Comment> commentsFor(UUID applicationId) {
        requireExists(applicationId);
        return comments.findByApplicationIdOrderByCreatedAtAsc(applicationId);
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> activityFor(UUID applicationId) {
        requireExists(applicationId);
        return activity.findByApplicationIdOrderByOccurredAtAsc(applicationId);
    }

    // ----------------------------------------------------------------- helpers

    private void requireExists(UUID applicationId) {
        if (!applications.existsById(applicationId)) {
            throw new NotFoundException("Application not found: " + applicationId);
        }
    }

    private void writeActivity(UUID applicationId, UUID actorId, String action,
                               Map<String, Object> payload, Instant occurredAt) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize activity payload for application {}", applicationId, e);
        }
        activity.save(new ActivityLog(UUID.randomUUID(), applicationId, actorId, action, json, occurredAt));
    }
}
