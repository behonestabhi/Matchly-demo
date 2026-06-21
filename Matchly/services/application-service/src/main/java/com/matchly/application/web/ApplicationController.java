package com.matchly.application.web;

import com.matchly.application.domain.Application;
import com.matchly.application.dto.ActivityResponse;
import com.matchly.application.dto.ApplicationResponse;
import com.matchly.application.dto.ApplyRequest;
import com.matchly.application.dto.CommentRequest;
import com.matchly.application.dto.CommentResponse;
import com.matchly.application.dto.StageChangeRequest;
import com.matchly.application.security.CurrentUserResolver;
import com.matchly.application.service.ApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Application / ATS REST API (API_CONTRACTS.md — Application/ATS Service).
 *
 * <p>Identity arrives via gateway-forwarded {@code X-User-Id} / {@code X-User-Roles}
 * headers. Role checks mirror the contract's RBAC annotations.
 */
@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String ROLE_RECRUITER = "RECRUITER";
    private static final String ROLE_HIRING_MANAGER = "HIRING_MANAGER";
    private static final String ROLE_ADMIN = "ADMIN";

    private final ApplicationService service;
    private final CurrentUserResolver currentUser;

    public ApplicationController(ApplicationService service, CurrentUserResolver currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    /** 🟦 Apply to a job → creates APPLIED, emits {@code ApplicationSubmitted}. */
    @PostMapping
    public ResponseEntity<ApplicationResponse> apply(@Valid @RequestBody ApplyRequest body,
                                                     HttpServletRequest request) {
        UUID candidateId = currentUser.requireUserId(request);
        Application created = service.apply(candidateId, body.jobId(), body.source(),
                correlationId(request));
        return ResponseEntity
                .created(URI.create("/api/v1/applications/" + created.getId()))
                .body(ApplicationResponse.from(created));
    }

    /** 🟦 Candidate's own applications + current stage (tracking). */
    @GetMapping("/me")
    public List<ApplicationResponse> myApplications(HttpServletRequest request) {
        UUID candidateId = currentUser.requireUserId(request);
        return service.findByCandidate(candidateId).stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    /** 🟩🟨 Pipeline board for a job. */
    @GetMapping
    public List<ApplicationResponse> pipeline(@RequestParam("jobId") UUID jobId,
                                              HttpServletRequest request) {
        currentUser.requireAnyRole(request, ROLE_RECRUITER, ROLE_HIRING_MANAGER, ROLE_ADMIN);
        return service.findByJob(jobId).stream()
                .map(ApplicationResponse::from)
                .toList();
    }

    /** 🟩🟨 Move stage → writes transition + activity, emits {@code StageChanged}. */
    @PatchMapping("/{id}/stage")
    public ApplicationResponse changeStage(@PathVariable("id") UUID id,
                                           @Valid @RequestBody StageChangeRequest body,
                                           HttpServletRequest request) {
        currentUser.requireAnyRole(request, ROLE_RECRUITER, ROLE_HIRING_MANAGER, ROLE_ADMIN);
        UUID actorId = currentUser.requireUserId(request);
        Application updated = service.changeStage(id, body.toStage(), body.reason(), actorId,
                correlationId(request));
        return ApplicationResponse.from(updated);
    }

    /** 🟩🟨 Add a recruiter comment. */
    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable("id") UUID id,
                                                      @Valid @RequestBody CommentRequest body,
                                                      HttpServletRequest request) {
        currentUser.requireAnyRole(request, ROLE_RECRUITER, ROLE_HIRING_MANAGER, ROLE_ADMIN);
        UUID authorId = currentUser.requireUserId(request);
        CommentResponse response = CommentResponse.from(
                service.addComment(id, authorId, body.body(), body.visibility()));
        return ResponseEntity
                .created(URI.create("/api/v1/applications/" + id + "/comments/" + response.id()))
                .body(response);
    }

    /** 🟩🟨 Activity / audit log for an application. */
    @GetMapping("/{id}/activity")
    public List<ActivityResponse> activity(@PathVariable("id") UUID id, HttpServletRequest request) {
        currentUser.requireAnyRole(request, ROLE_RECRUITER, ROLE_HIRING_MANAGER, ROLE_ADMIN);
        return service.activityFor(id).stream()
                .map(ActivityResponse::from)
                .toList();
    }

    private String correlationId(HttpServletRequest request) {
        return request.getHeader(CORRELATION_HEADER);
    }
}
